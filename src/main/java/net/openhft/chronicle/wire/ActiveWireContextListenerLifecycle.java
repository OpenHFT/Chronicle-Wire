/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Stateful lifecycle allocated only for a configured context listener.
 *
 * <p>If the callback commits partial output and then throws, it is not retried in the same
 * context.</p>
 */
final class ActiveWireContextListenerLifecycle implements WireContextListenerLifecycle {
    private final Class<?> writerType;
    private final MarshallableOut.ContextListener<?> listener;
    private boolean started;
    private boolean notified;

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
     * records. The notified flag is latched before invoking user code.
     */
    @Override
    public void beforeDocument(AbstractWire wire, boolean metaData) {
        started = true;
        if (metaData || notified)
            return;

        notified = true;
        notifyListener(wire);
    }

    @Override
    public void resetContext() {
        notified = false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void notifyListener(AbstractWire wire) {
        ((MarshallableOut.ContextListener) listener).onNewContext(wire.methodWriter(writerType));
    }
}
