/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.wire.WireType.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// This class is for testing different wire formats
public class ReadAnyWireTest extends WireTestCommon {

    // A test case to test the TEXT wire format
    @Test
    @DisplayName("READ_ANY reads values written by text wire")
    public void testReadAny() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for READ_ANY text wire test");

        // Create a buffer to hold wire data
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        // Write to the buffer using TEXT wire format
        final Wire wire = TEXT.apply(bytes);
        wire.write((() -> "hello")).text("world");

        // Read from the buffer and validate
        Assertions.assertEquals("world", READ_ANY.apply(bytes).read(() -> "hello").text(),
                "READ_ANY should read text wire values");

        // Release the buffer resources
        bytes.releaseLast();
    }

    // Another test for the TEXT wire format
    @Test
    @DisplayName("READ_ANY reads first text wire entry")
    public void testCreateReadAnyFirstTextWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for READ_ANY first text wire test");

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        TEXT.apply(bytes).write((() -> "hello")).text(expected);
        Assertions.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text(),
                "READ_ANY should read the first text wire entry");
        bytes.releaseLast();
    }

    // Test the BINARY wire format
    @Test
    @DisplayName("READ_ANY reads first binary wire entry")
    public void testCreateReadAnyFirstBinaryWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for READ_ANY first binary wire test");

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assertions.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text(),
                "READ_ANY should read the first binary wire entry");
        bytes.releaseLast();
    }

    // Test the JSON wire format
    @Test
    @DisplayName("READ_ANY reads first JSON wire entry")
    public void testCreateReadAnyFirstJSONWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for READ_ANY first JSON wire test");

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        JSON.apply(bytes).write((() -> "hello")).text(expected);
        Assertions.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text(),
                "READ_ANY should read the first JSON wire entry");
        bytes.releaseLast();
    }

    // Test the FIELDLESS_BINARY wire format, but it's currently ignored due to some issues that need to be resolved
    @Test
    @Disabled("Disabled until fieldless binary detection is fixed")
    @DisplayName("READ_ANY reads first fieldless binary wire entry")
    public void testCreateReadAnyFirstFIELDLESS_BINARYWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for READ_ANY fieldless binary wire test");

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        FIELDLESS_BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assertions.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text(),
                "READ_ANY should read the first fieldless binary wire entry");
        bytes.releaseLast();
    }
}
