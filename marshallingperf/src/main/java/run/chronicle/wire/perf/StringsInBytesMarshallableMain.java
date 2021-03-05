package run.chronicle.wire.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.Wires;

public class StringsInBytesMarshallableMain {

    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        WithStrings n = new WithStrings("1", "12", "123", "1234", "123456", "1234567", "123456789", "12345678901");

        WithStrings n2 = new WithStrings();
        Bytes bytes = Bytes.elasticByteBuffer();

        for (int i = -20_000; i < 100_000_000; i++) {
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


    static class WithStrings extends BytesInBinaryMarshallable {
        String a, b, c, d, e, f, g, h;

        public WithStrings() {
        }

        public WithStrings(String a, String b, String c, String d, String e, String f, String g, String h) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
            this.g = g;
            this.h = h;
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException {
            final Bytes<?> bytes2 = Wires.acquireBytes();
            for (int i = 0; i < 8; i++)
                bytes.read8bit(bytes2);
/*
            a = bytes.read8bit();
            b = bytes.read8bit();
            c = bytes.read8bit();
            d = bytes.read8bit();
            e = bytes.read8bit();
            f = bytes.read8bit();
            g = bytes.read8bit();
            h = bytes.read8bit();
*/
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
