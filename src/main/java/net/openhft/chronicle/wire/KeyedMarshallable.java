/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link Marshallable} that exposes a portion of its state as a key.
 * Useful for map-like collections where objects are identified by part of their
 * content.
 */
public interface KeyedMarshallable {

    /**
     * Writes the key of the current instance into the provided {@code Bytes} object.
     * This default implementation utilizes the {@code Wires.writeKey} method.
     *
     * @param bytes The {@code Bytes} object into which the key of the current instance is written.
     */
    @SuppressWarnings("rawtypes")
    default void writeKey(@NotNull Bytes<?> bytes) {
        Wires.writeKey(this, bytes);
    }
}
