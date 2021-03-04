package run.chronicle.wire.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

public class BytesInBytesMarshallableMain {

    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        WithBytes n = new WithBytes("1", "12", "123", "1234", "123456", "1234567", "123456789", "12345678901");

        WithBytes n2 = new WithBytes();
        Bytes bytes = Bytes.elasticByteBuffer();

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


    static class WithBytes extends BytesInBinaryMarshallable {
        Bytes a, b, c, d, e, f, g, h;

        public WithBytes() {
            a = Bytes.elasticHeapByteBuffer(64);
            b = Bytes.elasticHeapByteBuffer(64);
            c = Bytes.elasticHeapByteBuffer(64);
            d = Bytes.elasticHeapByteBuffer(64);
            e = Bytes.elasticHeapByteBuffer(64);
            f = Bytes.elasticHeapByteBuffer(64);
            g = Bytes.elasticHeapByteBuffer(64);
            h = Bytes.elasticHeapByteBuffer(64);
        }

        public WithBytes(String a, String b, String c, String d, String e, String f, String g, String h) {
            this.a = Bytes.from(a);
            this.b = Bytes.from(b);
            this.c = Bytes.from(c);
            this.d = Bytes.from(d);
            this.e = Bytes.from(e);
            this.f = Bytes.from(f);
            this.g = Bytes.from(g);
            this.h = Bytes.from(h);
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException {
            bytes.read8bit(a);
            bytes.read8bit(b);
            bytes.read8bit(c);
            bytes.read8bit(d);
            bytes.read8bit(e);
            bytes.read8bit(f);
            bytes.read8bit(g);
            bytes.read8bit(h);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.write8bit(a);
            bytes.write8bit(b);
            bytes.write8bit(c);
            bytes.write8bit(d);
            bytes.write8bit(e);
            bytes.write8bit(f);
            bytes.write8bit(g);
            bytes.write8bit(h);
        }
    }
}
