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
final class ActiveWireContextListenerLifecycle implements WireContextListenerLifecycle {
    private final Class<?> writerType;
    private final MarshallableOut.ContextListener<?> listener;
    private boolean started;
    private State state = State.READY;
    private boolean suppliedDocumentOpening;
    private boolean listenerRolledBack;
    private Throwable failure;

    ActiveWireContextListenerLifecycle(Class<?> writerType, MarshallableOut.ContextListener<?> listener) {
        this.writerType = writerType;
        this.listener = listener;
    }

    @Override
    public boolean started() {
        /// WireContextListenerLifecycleTest#contextListenerWaitsForDataAfterMetadataAndWritesDtoOnce
        /// demonstrates that leading metadata starts registration even though notification is deferred.
        return started;
    }

    /**
     * Fires once per context before the first data document. Metadata may precede the context
     * records.
     */
    @Override
    public void beforeDocument(AbstractWire wire, boolean metaData) {
        started = true;
        /// WireContextListenerLifecycleTest#metadataCannotBypassAFailedListener demonstrates that
        /// FAILED is checked before the metadata exemption and preserves the original cause.
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
                /// WireContextListenerLifecycleTest#listenerCannotReenterThroughOuterWire demonstrates
                /// that IN_PROGRESS must be visible before invoking user code.
                state = State.IN_PROGRESS;
                listenerRolledBack = false;
                try {
                    notifyListener(wire);
                    /// WireContextListenerLifecycleTest#listenerRollbackFailsClosedAcrossWires demonstrates
                    /// that an explicit listener rollback cannot be reported as successful context output.
                    if (listenerRolledBack)
                        throw new IllegalStateException(
                                "Context listener rolled back its output document");
                    /// WireContextListenerLifecycleTest#incompleteChainedListenerOutputFailsClosedAcrossWires
                    /// demonstrates that returning with an open chain must fail before application output.
                    if (!wire.writingIsComplete()) {
                        throw new IllegalStateException(
                                "Context listener returned with an incomplete chained document");
                    }
                    state = State.SUCCEEDED;
                } catch (Throwable throwable) {
                    /// WireContextListenerLifecycleTest#listenerFailurePoisonsCurrentContextUntilReset
                    /// demonstrates one rollback/diagnostic transition and no same-context callback retry.
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
                /// WireContextListenerLifecycleTest#listenerCannotReenterThroughOuterWire demonstrates that
                /// only the one-shot supplied-writer opening may cross the common Wire boundary.
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
        /// WireContextListenerLifecycleTest#listenerCannotClearTheOuterWire demonstrates that reset or
        /// clear cannot erase structural state while listener code is executing.
        if (state == State.IN_PROGRESS)
            throw new IllegalStateException("Cannot reset a wire while its context listener is running");
    }

    @Override
    public void resetContext() {
        /// WireContextListenerLifecycleTest#resetReusesListenerForTheNextOutputContext demonstrates that
        /// reset clears failure and per-context state while retaining the configured listener.
        state = State.READY;
        listenerRolledBack = false;
        failure = null;
    }

    @Override
    public void documentRolledBack() {
        /// WireContextListenerLifecycleTest#listenerRollbackFailsClosedAcrossWires and
        /// #applicationSerializationFailurePoisonsSuccessfulContextUntilReset distinguish rollback
        /// during notification from rollback after a successful context.
        if (state == State.IN_PROGRESS) {
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
        /// WireContextListenerLifecycleTest#listenerFailurePoisonsCurrentContextUntilReset demonstrates
        /// one transition to FAILED; diagnostics are emitted only at that transition, without payload data.
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
        /// WireContextListenerLifecycleTest#suppliedMethodWriterCanBeRetainedAfterNotification and
        /// #proxyFallbackUsesTheSuppliedListenerOutput demonstrate a normal retained writer bound
        /// to the listener-only output for generated and reflective paths.
        final VanillaMethodWriterBuilder builder =
                (VanillaMethodWriterBuilder) wire.methodWriterBuilder(writerType);
        builder.marshallableOut(new ListenerOutput(wire));
        ((MarshallableOut.ContextListener) listener).onNewContext(builder.build());
    }

    private final class ListenerOutput implements MarshallableOut {
        private final AbstractWire wire;

        private ListenerOutput(AbstractWire wire) {
            this.wire = wire;
        }

        @NotNull
        @Override
        public DocumentContext writingDocument(boolean metaData) {
            /// WireContextListenerLifecycleTest#suppliedMethodWriterCanBeRetainedAfterNotification
            /// demonstrates that supplied writes use the ordinary document boundary.
            return openDocument(metaData, false);
        }

        @NotNull
        @Override
        public DocumentContext acquireWritingDocument(boolean metaData) {
            /// WireContextListenerLifecycleTest#allConcreteWiresInvokeListenerBeforeFirstDataDocument
            /// exercises acquisition through the same one-shot supplied-writer boundary.
            return openDocument(metaData, true);
        }

        private DocumentContext openDocument(boolean metaData, boolean acquire) {
            /// WireContextListenerLifecycleTest#listenerCannotReenterThroughOuterWire demonstrates that
            /// one supplied opening is allowed but nested/captured outer-Wire openings are rejected.
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
        /// WireContextListenerLifecycleTest#listenerCannotReenterThroughOuterWire demonstrates that
        /// the supplied writer's permission is consumed at exactly one AbstractWire boundary.
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
