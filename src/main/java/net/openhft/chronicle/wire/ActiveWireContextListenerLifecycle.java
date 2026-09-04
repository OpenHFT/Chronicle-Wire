/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

/**
 * Stateful lifecycle allocated only for a configured context listener.
 *
 * <p>A failed callback poisons the current context. It is not retried because it may already have
 * committed context records, and application data is rejected until the wire is reset.</p>
 */
final class ActiveWireContextListenerLifecycle<T> implements WireContextListenerLifecycle {
    private final Class<T> writerType;
    private final MarshallableOut.ContextListener<? super T> listener;
    private boolean started;
    private State state = State.READY;
    private boolean suppliedDocumentOpening;
    private boolean listenerRolledBack;
    private Throwable failure;

    ActiveWireContextListenerLifecycle(Class<T> writerType, MarshallableOut.ContextListener<? super T> listener) {
        this.writerType = writerType;
        this.listener = listener;
    }

    @Override
    public boolean started() {
        return started;
    }

    /**
     * Fires once per context before the first data document. Metadata may precede the context
     * records.
     */
    @Override
    public void beforeDocument(AbstractWire wire, boolean metaData) {
        started = true;
        // A listener failure poisons the whole output context. Metadata must not bypass that
        // boundary and create a stream whose later application records lack valid context.
        //! WireContextListenerLifecycleTest#metadataCannotBypassAFailedListener demonstrates why
        //! failure is rejected before the metadata exemption: otherwise metadata could advance a
        //! poisoned stream without valid context and obscure the original callback failure.
        if (state == State.FAILED)
            throw new IllegalStateException(
                    "Context listener failed for the current output context", failure);
        if (metaData) {
            if (state == State.IN_PROGRESS && !consumeSuppliedDocumentOpening())
                throw new IllegalStateException(
                        "Only the supplied context writer may write while the context listener is running");
            return;
        }
        switch (state) {
            case READY:
                //! WireContextListenerLifecycleTest#retainedSuppliedWriterCanStartNextContextAfterReset
                //! demonstrates that a retained writer may open the next context first; consume its outer
                //! opening so the newly re-armed listener can open its own supplied context document.
                consumeSuppliedDocumentOpening();
                // Publish re-entrancy state before invoking user code; otherwise a listener can
                // recursively open an ordinary document ahead of its own context record.
                //! WireContextListenerLifecycleTest#listenerCannotReenterThroughOuterWire demonstrates
                //! that IN_PROGRESS must be visible before calling user code; publishing it afterward
                //! admits an outer-Wire document ahead of the required context record.
                state = State.IN_PROGRESS;
                listenerRolledBack = false;
                try {
                    notifyListener(wire);
                    //! WireContextListenerLifecycleTest#listenerRollbackFailsClosedAcrossWires
                    //! requires an explicit rollback requested by callback code to fail the
                    //! notification even after rollback has closed the supplied document.
                    if (listenerRolledBack)
                        throw new IllegalStateException(
                                "Context listener rolled back its output document");
                    //! WireContextListenerLifecycleTest#incompleteChainedListenerOutputFailsClosedAcrossWires
                    //! requires the callback to finish every supplied chain before application
                    //! data can follow it; otherwise the next write can become part of context.
                    if (!wire.writingIsComplete()) {
                        throw new IllegalStateException(
                                "Context listener returned with an incomplete chained document");
                    }
                    state = State.SUCCEEDED;
                } catch (Throwable throwable) {
                    //! WireContextListenerLifecycleTest#listenerFailurePoisonsCurrentContextUntilReset
                    //! requires the callback failure to remain primary while any incomplete
                    //! supplied document is rolled back. Retrying after partial context output can
                    //! duplicate records, so cleanup failure is suppressed and the state is FAILED.
                    try {
                        wire.rollbackIfNotComplete();
                    } catch (Throwable rollbackFailure) {
                        throwable.addSuppressed(rollbackFailure);
                    }
                    failure = throwable;
                    state = State.FAILED;
                    logFailureOnce(wire, throwable);
                    throw Jvm.rethrow(throwable);
                }
                return;
            case IN_PROGRESS:
                if (consumeSuppliedDocumentOpening())
                    return;
                throw new IllegalStateException(
                        "Only the supplied context writer may write while the context listener is running");
            case SUCCEEDED:
                return;
            case FAILED:
                throw new AssertionError("FAILED handled before metadata dispatch");
            default:
                throw new AssertionError(state);
        }
    }

    @Override
    public void checkCanResetContext() {
        if (state == State.IN_PROGRESS)
            throw new IllegalStateException("Cannot reset a wire while its context listener is running");
    }

    @Override
    public void resetContext() {
        //! WireContextListenerLifecycleTest#resetReusesListenerForTheNextOutputContext requires a
        //! completed or failed context to re-arm only when the owning Wire completes its reset.
        state = State.READY;
        listenerRolledBack = false;
        failure = null;
    }

    @Override
    public void documentRolledBack() {
        if (state == State.IN_PROGRESS) {
            //! WireContextListenerLifecycleTest#listenerRollbackFailsClosedAcrossWires requires
            //! callback rollback intent to survive document cleanup until beforeDocument can
            //! reject the notification.
            listenerRolledBack = true;
            return;
        }
        if (state != State.SUCCEEDED)
            return;
        //! WireContextListenerLifecycleTest#applicationSerializationFailurePoisonsSuccessfulContextUntilReset
        //! requires rollback of a later application document to invalidate the successful
        //! lifecycle: truncation may also have removed the context records it depended on.
        failure = new IllegalStateException(
                "Application rollback may have removed the context records for this output context");
        state = State.FAILED;
    }

    private void logFailureOnce(AbstractWire wire, Throwable throwable) {
        Jvm.warn().on(ActiveWireContextListenerLifecycle.class,
                "Context listener failed: writerType=" + writerType.getName()
                        + ", listenerType=" + listener.getClass().getName()
                        + ", outputType=" + wire.getClass().getName()
                        + ", outputIdentity=0x" + Integer.toHexString(System.identityHashCode(wire))
                        + ", contextCount=" + wire.contextCount(),
                throwable);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void notifyListener(AbstractWire wire) {
        final VanillaMethodWriterBuilder<T> builder = (VanillaMethodWriterBuilder<T>) wire.methodWriterBuilder(writerType);
        builder.marshallableOut(new ListenerOutput(wire));
        listener.onNewContext(builder.build());
    }

    private final class ListenerOutput implements MarshallableOut {
        private final AbstractWire wire;

        private ListenerOutput(AbstractWire wire) {
            this.wire = wire;
        }

        @NotNull
        @Override
        public DocumentContext writingDocument(boolean metaData) {
            return openDocument(metaData, false);
        }

        @NotNull
        @Override
        public DocumentContext acquireWritingDocument(boolean metaData) {
            return openDocument(metaData, true);
        }

        private DocumentContext openDocument(boolean metaData, boolean acquire) {
            //! WireContextListenerLifecycleTest#listenerCannotReenterThroughOuterWire and
            //! #incompleteChainedListenerOutputFailsClosedAcrossWires require exactly one supplied
            //! opening token. A nested acquisition before the outer holder is established can
            //! recurse into notification or bind competing context documents.
            if (suppliedDocumentOpening)
                throw new IllegalStateException("A supplied context document is already being opened");
            suppliedDocumentOpening = true;
            try {
                return acquire
                        ? wire.acquireWritingDocument(metaData)
                        : wire.writingDocument(metaData);
            } finally {
                suppliedDocumentOpening = false;
            }
        }
    }

    private boolean consumeSuppliedDocumentOpening() {
        if (!suppliedDocumentOpening)
            return false;
        suppliedDocumentOpening = false;
        return true;
    }

    private enum State {
        READY,
        IN_PROGRESS,
        SUCCEEDED,
        FAILED
    }
}
