//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

/**
 * Defines a contract for components that can serialise method calls to a {@link MarshallableOut} target.
 * Typically implemented by proxies created via {@link MarshallableOut#methodWriter(Class, Class...)} or
 * {@link VanillaMethodWriterBuilder}. Implementations should ensure that method
 * invocations on the writer result in corresponding messages being written to the configured {@code MarshallableOut}.
 *
 * @see MarshallableOut#methodWriter(Class, Class...)
 * @see VanillaMethodWriterBuilder
 */
public interface MethodWriter {

    /**
     * Transforms or serializes the internal state of the implementer to the provided
     * {@link MarshallableOut} instance. Implementers should handle the logic for
     * extracting their state and using the methods available on the {@code out} parameter
     * to output this state in the appropriate format.
     *
     * @param out The {@link MarshallableOut} instance to which the implementer's state should be written.
     */
    void marshallableOut(MarshallableOut out);
}
