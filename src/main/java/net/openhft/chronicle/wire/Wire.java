/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.annotation.SingleThreaded;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the standard interface for sequentially writing to and reading from a Bytes stream.
 * Implementations of this interface should ensure single-threaded access and avoid method chaining.
 *
 * <p>This interface combines the capabilities of both {@link WireIn} and {@link WireOut} interfaces.
 */
@SingleThreaded
@DontChain
public interface Wire extends WireIn, WireOut {
    /**
     * Factory method to create a new YamlWire instance that writes to an on-heap Bytes object.
     *
     * @return A YamlWire instance configured to write to an on-heap Bytes object.
     */
    static Wire newYamlWireOnHeap() {
        return new YamlWire();
    }

    /**
     * Set the header number for the Wire. Concrete implementations should ensure they provide the expected behavior for this method.
     *
     * @param headerNumber The header number to be set.
     * @return The current Wire instance, often for method chaining.
     */
    @Override
    @NotNull
    Wire headerNumber(long headerNumber);

    /**
     * Indicates whether tuples should be generated.
     * By default, this method returns the value of Wires.GENERATE_TUPLES
     *
     * @return true if tuples should be generated, false otherwise.
     */
    @Deprecated(/* to be removed in 2027 */)
    default boolean generateTuples() {
        return Wires.GENERATE_TUPLES;
    }

    /**
     * Sets whether tuples should be generated.
     *
     * @param generateTuples true to enable tuple generation, false to disable it.
     * @return this wire for chaining
     */
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    default Wire generateTuples(boolean generateTuples) {
        return this;
    }
}
