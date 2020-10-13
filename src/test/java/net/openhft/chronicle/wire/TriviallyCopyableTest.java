package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.junit.Test;

import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;

public class TriviallyCopyableTest {

    static void doTest(BiConsumer<Bytes, AA> read, BiConsumer<Bytes, AA> write) {
        Bytes bytes = Bytes.allocateDirect(40);
        AA aa = new AA((byte) 1, (byte) 2, true, false, 'Y', (short) 6, 7, 8, 9, 10);
        write.accept(bytes, aa);
        AA a2 = ObjectUtils.newInstance(AA.class);
        read.accept(bytes, a2);
        assertEquals(aa, a2);
        bytes.releaseLast();
    }

    @Test
    public void unsafe2() {
        doTest((b, a) -> a.readMarshallable(b), (b, a) -> a.writeMarshallable(b));
    }

    static class AA extends BytesInBinaryMarshallable {
        static final int FORMAT = 1;

        // natural order on a 64-bit JVM.
        int i;
        double d;
        long l;
        float f;
        short s;
        char ch;
        byte b1, b2;
        boolean flag, flag2;

        public AA(byte b1, byte b2, boolean flag, boolean flag2, char ch, short s, float f, int i, double d, long l) {
            this.b1 = b1;
            this.b2 = b2;
            this.flag = flag;
            this.flag2 = flag2;
            this.ch = ch;
            this.s = s;
            this.f = f;
            this.i = i;
            this.d = d;
            this.l = l;
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException {
            int id = (int) bytes.readStopBit();
            switch (id) {
                case FORMAT:
                    if (OS.is64Bit())
                        bytes.unsafeReadObject(this, 32);
                    else
                        readMarshallable1(bytes);
                    return;
                default:
                    throw new IORuntimeException("Unknown format " + id);
            }
        }

        void readMarshallable1(BytesIn bytes) throws IORuntimeException {
            i = bytes.readInt();
            d = bytes.readDouble();
            l = bytes.readLong();
            f = bytes.readFloat();
            s = bytes.readShort();
            ch = bytes.readChar();
            b1 = bytes.readByte();
            b2 = bytes.readByte();
            flag = bytes.readBoolean();
            flag2 = bytes.readBoolean();
        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.writeStopBit(FORMAT);
            if (OS.is64Bit())
                bytes.unsafeWriteObject(this, 32);
            else
                writeMarshallable1(bytes);
        }

        void writeMarshallable1(BytesOut bytes) {
            bytes.writeInt(i);
            bytes.writeDouble(d);
            bytes.writeLong(l);
            bytes.writeFloat(f);
            bytes.writeShort(s);
            bytes.writeChar(ch);
            bytes.writeByte(b1);
            bytes.writeByte(b2);
            bytes.writeBoolean(flag);
            bytes.writeBoolean(flag2);
        }
    }
}
