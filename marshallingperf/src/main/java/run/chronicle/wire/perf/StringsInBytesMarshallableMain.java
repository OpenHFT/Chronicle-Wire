package run.chronicle.wire.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

import static run.chronicle.wire.perf.BytesInBytesMarshallableMain.histoOut;

/*
.2.21ea74
read: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.542 / 0.566  0.586 / 0.610  0.690 / 1.092  16.1 / 19.9  29.4 / 29.9  32.9 / 33.9 - 178
write: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.204 / 0.212  0.222 / 0.230  0.261 / 0.373  0.461 / 15.8  19.6 / 28.9  29.4 / 32.4 - 143

.latest
read: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.233 / 0.245  0.253 / 0.259  0.303 / 0.439  0.574 / 15.9  21.2 / 29.1  29.2 / 32.6 - 58.2
write: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.062 / 0.066  0.068 / 0.073  0.081 / 0.126  0.172 / 0.305  15.6 / 19.4  28.7 / 29.0 - 41.9
 */

public class StringsInBytesMarshallableMain {

    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        WithStrings n = new WithStrings("1", "12", "123", "1234", "123456", "1234567", "123456789", "12345678901");

        WithStrings n2 = new WithStrings();
        Bytes<?> bytes = Bytes.allocateElasticDirect(128);

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

        histoOut("read", StringsInBytesMarshallableMain.class, readHist);
        histoOut("write", StringsInBytesMarshallableMain.class, writeHist);
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
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException {
            a = bytes.read8bit();
            b = bytes.read8bit();
            c = bytes.read8bit();
            d = bytes.read8bit();
            e = bytes.read8bit();
            f = bytes.read8bit();
            g = bytes.read8bit();
            h = bytes.read8bit();
        }

        @Override
        public void writeMarshallable(BytesOut<?> bytes) {
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
