//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.wire.WireType.*;
import static org.junit.Assume.assumeFalse;

// This class is for testing different wire formats
public class ReadAnyWireTest extends WireTestCommon {

    // A test case to test the TEXT wire format
    @Test
    public void testReadAny() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Create a buffer to hold wire data
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        // Write to the buffer using TEXT wire format
        final Wire wire = TEXT.apply(bytes);
        wire.write((() -> "hello")).text("world");

        // Read from the buffer and validate
        Assert.assertEquals("world", READ_ANY.apply(bytes).read(() -> "hello").text());

        // Release the buffer resources
        bytes.releaseLast();
    }

    // Another test for the TEXT wire format
    @Test
    public void testCreateReadAnyFirstTextWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        TEXT.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
        bytes.releaseLast();
    }

    // Test the BINARY wire format
    @Test
    public void testCreateReadAnyFirstBinaryWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
        bytes.releaseLast();
    }

    // Test the JSON wire format
    @Test
    public void testCreateReadAnyFirstJSONWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        JSON.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
        bytes.releaseLast();
    }

    // Test the FIELDLESS_BINARY wire format, but it's currently ignored due to some issues that need to be resolved
    @Test
    @Ignore("TODO FIX")
    public void testCreateReadAnyFirstFIELDLESS_BINARYWire() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        FIELDLESS_BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
        bytes.releaseLast();
    }
}
