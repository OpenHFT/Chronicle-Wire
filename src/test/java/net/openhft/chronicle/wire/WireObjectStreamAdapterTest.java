/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        assertEquals(4, input.available());
        assertEquals(1, input.skip(1));

        byte[] dest = new byte[5];
        int read = input.read(dest, 1, dest.length - 1);
        assertEquals(3, read);
        assertArrayEquals(new byte[]{0, 8, 7, 6, 0}, dest);
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
