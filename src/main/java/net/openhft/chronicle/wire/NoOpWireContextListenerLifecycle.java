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
        //! WireContextListenerLifecycleTest#listenerFreeWireCannotBeConfiguredAfterFirstUse demonstrates
        //! why SET reports started even though listener-free notification itself is a no-op.
        return this == SET;
    }

    @Override
    public void beforeDocument(AbstractWire wire, boolean metaData) {
        //! WireContextListenerLifecycleTest#listenerFreeWireCannotBeConfiguredAfterFirstUse demonstrates
        //! that the UNSET-to-SET transition belongs at the caller; this shared callback stays allocation-free.
    }
}
