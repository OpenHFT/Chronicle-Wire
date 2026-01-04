/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

/**
 * Defines a specialized handler for dealing with ChronicleEvent instances.
 * This interface extends the generic EventHandler to specifically handle
 * ChronicleEvent objects.
 */
public interface ChronicleEventHandler extends EventHandler<ChronicleEvent> {

    /**
     * Handles a ChronicleEvent and applies any downstream routing or validation.
     *
     * @param event The ChronicleEvent instance to be handled.
     */
    @Override
    void event(ChronicleEvent event);
}
