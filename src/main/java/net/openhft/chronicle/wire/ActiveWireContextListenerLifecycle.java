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
                startContext(wire);
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

    // User callbacks can throw Errors or sneaky checked exceptions. Preserve the original failure
    // while rolling back, then poison this context so partially written records cannot be retried.
    @SuppressWarnings("java:S1181")
    private void startContext(AbstractWire wire) {
        // A writer retained across reset can open this context and then be used again inside the listener.
        consumeSuppliedDocumentOpening();
        // Publish re-entrancy state before invoking user code; otherwise a listener can
        // recursively open an ordinary document ahead of its own context record.
        state = State.IN_PROGRESS;
        listenerRolledBack = false;
        try {
            notifyListener(wire);
            if (listenerRolledBack)
                throw new IllegalStateException(
                        "Context listener rolled back its output document");
            // An open chain would make the next application write part of the context document.
            if (!wire.writingIsComplete()) {
                throw new IllegalStateException(
                        "Context listener returned with an incomplete chained document");
            }
            state = State.SUCCEEDED;
        } catch (Throwable throwable) {
            // Retrying a partially written context could duplicate records.
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
    }

    @Override
    public void checkCanResetContext() {
        if (state == State.IN_PROGRESS)
            throw new IllegalStateException("Cannot reset a wire while its context listener is running");
    }

    @Override
    public void resetContext() {
        state = State.READY;
        listenerRolledBack = false;
        failure = null;
    }

    @Override
    public void documentRolledBack() {
        if (state == State.IN_PROGRESS) {
            // Remember the rollback even if the listener closes the document before returning.
            listenerRolledBack = true;
            return;
        }
        if (state != State.SUCCEEDED)
            return;
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
