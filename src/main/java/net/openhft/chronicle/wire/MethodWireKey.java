/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * The {@code MethodWireKey} class represents a key within a wire format, extending the
 * {@link BytesInBinaryMarshallable} class and implementing the {@link WireKey} interface.
 * Each instance of {@code MethodWireKey} has a unique name and code combination. The name,
 * if not provided, defaults to the string representation of the code.
 * <p>
 * This class can be particularly useful in scenarios where wire keys are required to be identified
 * both by a textual name and a numeric code.
 */
public class MethodWireKey extends BytesInBinaryMarshallable implements WireKey {

    // The name of the wire key
    private final String name;

    // The numeric code representing the wire key
    private final int code;

    /**
     * Constructs a new {@code MethodWireKey} with the provided name and code.
     *
     * @param name The name of the wire key.
     * @param code The numeric code representing the wire key.
     */
    public MethodWireKey(String name, int code) {
        this.name = name;
        this.code = code;
    }

    @NotNull
    @Override
    public String name() {
        return name == null ? Integer.toString(code) : name;
    }

    @Override
    public int code() {
        return code;
    }
}
