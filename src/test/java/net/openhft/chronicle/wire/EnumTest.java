/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * Tests for marshalling and unmarshalling of Enum using Wire.
 */
public class EnumTest extends WireTestCommon {
    @Before
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    /**
     * Tests serialization and deserialization of the TestEnum enumeration.
     */
    @Test
    public void testEnum() {
        // Expecting an exception regarding enum handling
        expectException("Treating class net.openhft.chronicle.wire.EnumTest$TestEnum as enum not WriteMarshallable");

        // Create a byte buffer to work with
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        try {
            // Initialize a Text wire using the byte buffer
            @NotNull Wire wire = WireType.TEXT.apply(bytes);

            // Write the TestEnum.INSTANCE to the wire
            wire.write("test")
                .object(TestEnum.INSTANCE);

            // Validate the serialized form of the TestEnum
            assertEquals("test: !net.openhft.chronicle.wire.EnumTest$TestEnum INSTANCE\n", wire.toString());

            // Create another Text wire with serialized TestEnum
            @NotNull TextWire wire2 = TextWire.from(
                    "test: !net.openhft.chronicle.wire.EnumTest$TestEnum {\n" +
                            "}\n");

            // Deserialize the TestEnum back from the wire
            @Nullable Object enumObject = wire2.read(() -> "test")
                .object();

            // Ensure original and read enum are the same
            Assert.assertSame(TestEnum.INSTANCE, enumObject);
        } finally {
            // Release the byte buffer resources
            bytes.releaseLast();
        }
    }

    /**
     * Enumeration used for testing purposes.
     * Implements Marshallable for Wire compatibility.
     */
    public enum TestEnum implements Marshallable {
        INSTANCE;

        // Read data from the wire, currently no implementation
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        }

        // Write data to the wire, currently no implementation
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
        }
    }
}
