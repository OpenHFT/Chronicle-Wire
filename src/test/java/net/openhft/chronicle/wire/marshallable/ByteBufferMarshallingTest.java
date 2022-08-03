/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.RawWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class ByteBufferMarshallingTest extends WireTestCommon {
    @Test
    public void writeReadByteBuffer() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire wire = new RawWire(bytes);

        AClass o1 = new AClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");

        o1.writeMarshallable(wire);

        AClass o2 = ObjectUtils.newInstance(AClass.class);
        o2.readMarshallable(wire);

        assertEquals(o1, o2);
        bytes.releaseLast();
    }

    @Test
    public void writeReadViaByteBuffer() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire wire = new RawWire(bytes);

        AClass o1 = new AClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");

        o1.writeMarshallable(wire);

        ByteBuffer bb = bytes.underlyingObject();
        bb.position((int) bytes.readPosition());
        bb.limit((int) bytes.readLimit());

        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        bytes2.ensureCapacity(bb.remaining());

        ByteBuffer bb2 = bytes2.underlyingObject();
        bb2.clear();

        bb2.put(bb);
        // read what we just wrote
        bytes2.readPosition(0);
        bytes2.readLimit(bb2.position());

        Wire wire2 = new RawWire(bytes2);

        AClass o2 = ObjectUtils.newInstance(AClass.class);
        o2.readMarshallable(wire2);
        assertEquals(o1, o2);
        bytes.releaseLast();
        bytes2.releaseLast();
    }

    @Test
    public void writeReadBytesViaByteBuffer() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        BClass o1 = new BClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");

        o1.writeMarshallable(bytes);

        ByteBuffer bb = bytes.underlyingObject();
        bb.position((int) bytes.readPosition());
        bb.limit((int) bytes.readLimit());

        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        bytes2.ensureCapacity(bb.remaining());

        ByteBuffer bb2 = bytes2.underlyingObject();
        bb2.clear();

        bb2.put(bb);
        // read what we just wrote
        bytes2.readPosition(0);
        bytes2.readLimit(bb2.position());

        BClass o2 = ObjectUtils.newInstance(BClass.class);
        o2.readMarshallable(bytes2);
        assertEquals(o1, o2);
        bytes.releaseLast();
        bytes2.releaseLast();
    }
}
