/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.InvalidMarshallableException;

/**
 * Functional interface invoked when a field name is parsed.
 * It is typically registered with a {@link WireParser} to define how to
 * deserialise the value associated with a specific field name.
 */
@FunctionalInterface
public interface WireParselet {

    /**
     * Consumes and processes a wire input based on a given character sequence
     * and a value input.
     *
     * @param s   the field name that matched this parselet
     * @param in  the {@link ValueIn} positioned at the value for {@code s}. Use it to
     *            deserialise the value
     * @throws InvalidMarshallableException if the value cannot be processed
     */
    void accept(CharSequence s, ValueIn in) throws InvalidMarshallableException;
}
