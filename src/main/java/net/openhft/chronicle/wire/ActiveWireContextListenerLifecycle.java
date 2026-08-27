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
    private int listenerWriteDepth;
    private Throwable failure;

    ActiveWireContextListenerLifecycle(Class<?> writerType, MarshallableOut.ContextListener<?> listener) {
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
        if (metaData) {
            if (state == State.IN_PROGRESS && listenerWriteDepth == 0)
                throw new IllegalStateException(
                        "Only the supplied context writer may write while the context listener is running");
            return;
        }
        switch (state) {
            case READY:
                state = State.IN_PROGRESS;
                try {
                    notifyListener(wire);
                    state = State.SUCCEEDED;
                } catch (Throwable throwable) {
                    failure = throwable;
                    state = State.FAILED;
                    throw Jvm.rethrow(throwable);
                }
                return;
            case IN_PROGRESS:
                if (listenerWriteDepth > 0)
                    return;
                throw new IllegalStateException(
                        "Only the supplied context writer may write while the context listener is running");
            case SUCCEEDED:
                return;
            case FAILED:
                throw new IllegalStateException(
                        "Context listener failed for the current output context", failure);
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
        state = State.READY;
        failure = null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void notifyListener(AbstractWire wire) {
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

        @Override
        public int contextCount() {
            return wire.contextCount();
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
            listenerWriteDepth++;
            try {
                return acquire
                        ? wire.acquireWritingDocument(metaData)
                        : wire.writingDocument(metaData);
            } finally {
                listenerWriteDepth--;
            }
        }
    }

    private enum State {
        READY,
        IN_PROGRESS,
        SUCCEEDED,
        FAILED
    }
}
