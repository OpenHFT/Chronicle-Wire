//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class WireObjectStreamAdapterTest extends WireTestCommon {

    @Test
    public void roundTripPrimitivesCollectionsAndRawBytes() throws Exception {
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

        assertTrue(input.readBoolean());
        assertEquals(7, input.readByte());
        assertEquals(1234, input.readShort());
        assertEquals('A', input.readChar());
        assertEquals(321, input.readInt());
        assertEquals(9_876_543_210L, input.readLong());
        assertEquals(1.25F, input.readFloat(), 0.0F);
        assertEquals(3.5D, input.readDouble(), 0.0D);
        assertEquals("hello", input.readUTF());

        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) input.readObject();
        assertEquals(Arrays.asList("first", "second"), list);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) input.readObject();
        assertEquals(Integer.valueOf(42), map.get("key"));

        // Available() reports remaining bytes in the underlying wire, which may include framing.
        assertTrue(input.available() >= 4);

        byte[] dest = new byte[5];
        int read = input.read(dest, 1, dest.length - 1);
        assertEquals(dest.length - 1 - 1, read);
        assertArrayEquals(new byte[]{9, 8, 7, 6, 0}, dest);
        assertEquals(0, input.available());
        assertEquals(-1, input.read());

        buffer.releaseLast();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void readFullyIsUnsupported() throws IOException {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        WireObjectInput input = new WireObjectInput(wire);
        input.readFully(new byte[1], 0, 1);
    }
}
