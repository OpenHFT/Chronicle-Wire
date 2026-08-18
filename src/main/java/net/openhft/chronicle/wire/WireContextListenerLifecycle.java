/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Internal lifecycle for a context listener attached to a wire.
 *
 * <p>The shared {@link #NO_OP} instance represents a wire whose first document has already started
 * without a listener. An {@link ActiveWireContextListenerLifecycle} is allocated only when the
 * advanced context-listener feature is configured.</p>
 */
interface WireContextListenerLifecycle {
    WireContextListenerLifecycle NO_OP = NoOpWireContextListenerLifecycle.INSTANCE;

    /**
     * @return whether an output document has already started
     */
    boolean started();

    /**
     * Invoked immediately before a wire starts an output document.
     *
     * @param wire     wire starting the document
     * @param metaData whether the document contains metadata
     */
    void beforeDocument(AbstractWire wire, boolean metaData);

    static <T> WireContextListenerLifecycle active(@NotNull Class<T> writerType,
                                                   @NotNull MarshallableOut.ContextListener<? super T> listener) {
        return new ActiveWireContextListenerLifecycle(requireNonNull(writerType), requireNonNull(listener));
    }
}

/**
 * Shared, allocation-free lifecycle used when no listener was configured before first use.
 *
 * <p>{@link AbstractWire} identifies this instance before dispatch, leaving this implementation as
 * the explicit frozen state while avoiding an interface call for listener-free writes.</p>
 */
enum NoOpWireContextListenerLifecycle implements WireContextListenerLifecycle {
    INSTANCE;

    @Override
    public boolean started() {
        return true;
    }

    @Override
    public void beforeDocument(AbstractWire wire, boolean metaData) {
        // Deliberately empty; useful to lifecycle tests and callers that dispatch directly.
    }
}

/**
 * Stateful lifecycle allocated only for a configured context listener.
 *
 * <p>The callback is latched before user code runs. If it commits partial output and then throws,
 * a later document therefore cannot duplicate the already committed context records.</p>
 */
final class ActiveWireContextListenerLifecycle implements WireContextListenerLifecycle {
    private final Class<?> writerType;
    private final MarshallableOut.ContextListener<?> listener;
    private boolean started;
    private boolean notified;
    private boolean notifying;

    ActiveWireContextListenerLifecycle(Class<?> writerType, MarshallableOut.ContextListener<?> listener) {
        this.writerType = writerType;
        this.listener = listener;
    }

    @Override
    public boolean started() {
        return started;
    }

    /**
     * Fires at most once, before the first data document. Metadata may precede the context records.
     * The notified flag is latched before invoking user code so a partial listener failure is not
     * retried and duplicated.
     */
    @Override
    public void beforeDocument(AbstractWire wire, boolean metaData) {
        started = true;
        if (metaData || notified || notifying)
            return;

        notified = true;
        notifying = true;
        try {
            notifyListener(wire);
        } finally {
            notifying = false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void notifyListener(AbstractWire wire) {
        ((MarshallableOut.ContextListener) listener).onNewContext(wire.methodWriter(writerType));
    }
}
