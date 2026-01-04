/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Class representing an object that implements Marshallable interface.
 * The object primarily deals with StringBuilder data and provides mechanisms
 * for reading and writing that data with Wire.
 */
public class MarshallableObj implements Marshallable {
    private final StringBuilder builder = new StringBuilder();

    // Clears the current content of the builder
    public void clear() {
        builder.setLength(0);
    }

    // Appends a character sequence to the builder
    public void append(CharSequence cs) {
        builder.append(cs);
    }

    // Reads the string value from the wire and sets it to the builder
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        builder.setLength(0);
        assertNotNull(wire.getValueIn().textTo(builder),
                "Wire input should provide text for builder");
    }

    // Writes the current string value from the builder to the wire
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.getValueOut().text(builder);
    }

    // Equality is based on the content of the builder
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @Nullable MarshallableObj that = (MarshallableObj) o;

        return builder.toString().contentEquals(that.builder);
    }

    // Hashcode derived from the content of the builder
    @Override
    public int hashCode() {
        return builder.toString().hashCode();
    }

    // String representation is the content of the builder
    @NotNull
    @Override
    public String toString() {
        return builder.toString();
    }
}
