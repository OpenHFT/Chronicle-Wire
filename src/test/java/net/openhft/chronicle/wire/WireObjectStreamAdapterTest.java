/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WireObjectStreamAdapterTest extends WireTestCommon {

    @Test
    @DisplayName("Wire object streams round trip primitives and collections")
    void roundTripPrimitivesCollectionsAndRawBytes() throws Exception {
        Bytes<?> buffer = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(buffer);

        WireObjectOutput output = new WireObjectOutput(wire);
        output.writeBoolean(true);
        output.writeByte(7);
        output.writeShort(1234);
        output.writeChar('A');
        output.writeInt(321);
        output.writeLong(9_876_543_210L);
        output.writeFloat(1.25F);
        output.writeDouble(3.5D);
        output.writeUTF("hello");
        output.writeObject(Arrays.asList("first", "second"));
        output.writeObject(Collections.singletonMap("key", 42));
        output.write(new byte[]{9, 8, 7, 6});

        wire.bytes().readPositionRemaining(0, wire.bytes().writePosition());
        WireObjectInput input = new WireObjectInput(wire);

        assertTrue(input.readBoolean(), "wire object stream should preserve boolean values");
        assertEquals(7, input.readByte(), "wire object stream should preserve byte values");
        assertEquals(1234, input.readShort(), "wire object stream should preserve short values");
        assertEquals('A', input.readChar(), "wire object stream should preserve char values");
        assertEquals(321, input.readInt(), "wire object stream should preserve int values");
        assertEquals(9_876_543_210L, input.readLong(), "wire object stream should preserve long values");
        assertEquals(1.25F, input.readFloat(), 0.0F, "wire object stream should preserve float values");
        assertEquals(3.5D, input.readDouble(), 0.0D, "wire object stream should preserve double values");
        assertEquals("hello", input.readUTF(), "wire object stream should preserve UTF string values");

        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) input.readObject();
        assertEquals(Arrays.asList("first", "second"), list, "wire object stream should preserve list collection contents");
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) input.readObject();
        assertEquals(42, map.get("key").intValue(), "wire object stream should preserve map collection contents");

        // Available() reports remaining bytes in the underlying wire, which may include framing.
        assertTrue(input.available() >= 4, "wire object stream should report available bytes for remaining raw data");

        byte[] dest = new byte[5];
        int read = input.read(dest, 1, dest.length - 1);
        assertEquals(dest.length - 1 - 1, read, "wire object stream should read correct number of raw bytes into buffer");
        assertArrayEquals(new byte[]{9, 8, 7, 6, 0}, dest, "wire object stream should preserve raw byte array data with correct offset");
        assertEquals(0, input.available(), "wire object stream should report zero available bytes after consuming all data");
        assertEquals(-1, input.read(), "wire object stream should return -1 when no more data is available");

        buffer.releaseLast();
    }

    @Test
    @DisplayName("Wire object input rejects readFully calls")
    void readFullyIsUnsupported() throws IOException {
        assertThrows(UnsupportedOperationException.class, () -> {
            Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
            WireObjectInput input = new WireObjectInput(wire);
            input.readFully(new byte[1], 0, 1);
        }, "readFully should throw UnsupportedOperationException");
    }
}
