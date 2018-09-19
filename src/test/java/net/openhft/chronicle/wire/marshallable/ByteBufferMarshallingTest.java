package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.RawWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class ByteBufferMarshallingTest {
    @Test
    public void writeReadByteBuffer() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire wire = new RawWire(bytes);

        AClass o1 = new AClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");

        o1.writeMarshallable(wire);

        AClass o2 = ObjectUtils.newInstance(AClass.class);
        o2.readMarshallable(wire);

        assertEquals(o1, o2);
        bytes.release();
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
        bytes.release();
        bytes2.release();
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
        bytes.release();
        bytes2.release();
    }
}
