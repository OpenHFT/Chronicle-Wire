/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.wire.WireType.*;

// This class is for testing different wire formats
public class ReadAnyWireTest extends WireTestCommon {

    // A test case to test the TEXT wire format
    @Test
    public void testReadAny() {
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
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        TEXT.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
        bytes.releaseLast();
    }

    // Test the BINARY wire format
    @Test
    public void testCreateReadAnyFirstBinaryWire() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
        bytes.releaseLast();
    }

    // Test the JSON wire format
    @Test
    public void testCreateReadAnyFirstJSONWire() {
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
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        @NotNull final String expected = "world";
        FIELDLESS_BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
        bytes.releaseLast();
    }
}
