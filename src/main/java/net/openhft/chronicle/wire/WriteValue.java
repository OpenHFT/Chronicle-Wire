/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.InvalidMarshallableException;

/**
 * Represents an entity capable of writing its value to a specified output format.
 * This interface is intended to be implemented by classes or lambdas that need
 * to serialize their state to a given value representation.
 * <p>
 * It's designed with the {@code @FunctionalInterface} annotation, suggesting that
 * it's primarily intended for lambda expressions and method references.
 * The {@code @DontChain} annotation indicates a recommendation against chaining
 * methods for implementations of this interface.
 */
@FunctionalInterface
@DontChain
public interface WriteValue {

    /**
     * Writes the current state of the implementing object as a value
     * to the provided {@link ValueOut} representation.
     *
     * @param out The output representation to write the value to.
     * @throws InvalidMarshallableException if any serialization error occurs.
     */
    void writeValue(ValueOut out) throws InvalidMarshallableException;
}
