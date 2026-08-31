/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Shared, allocation-free lifecycle for listener-free wires. {@link #UNSET} allows configuration;
 * {@link #SET} records that the first document has started. The empty callback is expected to be
 * inlined on ordinary writes.
 */
enum NoOpWireContextListenerLifecycle implements WireContextListenerLifecycle {
    UNSET,
    SET;

    @Override
    public boolean started() {
        return this == SET;
    }

    @Override
    public void beforeDocument(AbstractWire wire, boolean metaData) {
    }
}
