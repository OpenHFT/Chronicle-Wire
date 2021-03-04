package run.chronicle.wire.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

public class PrimitivesInBytesMarshallableMain {

    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        WithPrimitives n = new WithPrimitives();

        WithPrimitives n2 = new WithPrimitives();
        Bytes bytes = Bytes.elasticByteBuffer(256);

        for (int i = -20_000; i < 10_000_000; i++) {
            bytes.clear();
            long start = System.nanoTime();
            n.writeMarshallable(bytes);
            long end = System.nanoTime();
            writeHist.sample(end - start);
            start = System.nanoTime();
            n2.readMarshallable(bytes);
            end = System.nanoTime();
            readHist.sample(end - start);
            if (i == 0) {
                readHist.reset();
                writeHist.reset();
            }
            if (i >= -1000)
                Thread.yield();
        }

        System.out.println("read: " + readHist.toLongMicrosFormat());
        System.out.println("write: " + writeHist.toLongMicrosFormat());
    }


    static class WithPrimitives extends BytesInBinaryMarshallable {
        boolean a;
        byte b;
        char c;
        short d;
        int e;
        float f;
        long g;
        double h;

        public WithPrimitives() {
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException {
            a = bytes.readBoolean();
            b = bytes.readByte();
            c = bytes.readChar();
            d = bytes.readShort();
            e = bytes.readInt();
            f = bytes.readFloat();
            g = bytes.readLong();
            h = bytes.readDouble();
        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.writeBoolean(a);
            bytes.writeByte(b);
            bytes.writeChar(c);
            bytes.writeShort(d);
            bytes.writeInt(e);
            bytes.writeFloat(f);
            bytes.writeLong(g);
            bytes.writeDouble(h);
        }
    }
}
