//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package run.chronicle.wire.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.jlbh.TeamCityHelper;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

/*
.2.21.ea74
read: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.222 / 0.229  0.238 / 0.253  0.361 / 0.534  0.746 / 16.1  20.4 / 29.5  30.0 / 32.4 - 98.6
write: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.184 / 0.193  0.201 / 0.213  0.305 / 0.459  0.626 / 15.9  19.6 / 29.4  29.9 / 32.1 - 97.5

.latest
read: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.145 / 0.151  0.163 / 0.223  0.309 / 0.566  0.630 / 15.5  19.3 / 28.5  28.7 / 31.6 - 70.9
write: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.079 / 0.082  0.090 / 0.122  0.177 / 0.309  0.427 / 1.108  15.8 / 22.5  28.6 / 28.9 - 53.4
*/

/**
 * Benchmarks the performance of {@link BytesInBinaryMarshallable} where each field
 * is stored as a block of bytes. Eight byte sequences are written to and read from
 * a direct {@link Bytes} buffer.
 *
 * <p>Latency for each read and write operation is recorded in a {@link Histogram}.
 * After a warm up phase the results are printed as histograms.
 */
public class BytesInBytesMarshallableMain {

    /**
     * Runs the benchmark.
     *
     * <p>The first twenty thousand iterations act as a warm up. Subsequent
     * iterations record the time to write and then read the test object.
     * Latency results are printed as histograms.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        WithBytes n = new WithBytes("1", "12", "123", "1234", "123456", "1234567", "123456789", "12345678901");

        WithBytes n2 = new WithBytes();
        Bytes<?> bytes = Bytes.allocateElasticDirect(128);

        // Negative iterations warm up the JIT to stabilise timings
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
            // Start timing after the warm up phase
            if (i == 0) {
                readHist.reset();
                writeHist.reset();
            }
            if (i >= -1000)
                // Allow other threads a chance to run during the warm up
                Thread.yield();
        }

        histoOut("read", BytesInBytesMarshallableMain.class, readHist);
        histoOut("write", BytesInBytesMarshallableMain.class, writeHist);
    }

    /**
     * Prints the histogram in a human readable form and emits the same data for
     * TeamCity builds.
     */
    static void histoOut(String msg, Class<?> clazz, Histogram histo) {
        System.out.println(msg + ": " + histo.toLongMicrosFormat());
        TeamCityHelper.histo(clazz.getSimpleName() + "." + msg, histo, System.out);
    }

    /**
     * Test object containing eight {@link Bytes} fields used for the benchmark.
     */
    static class WithBytes extends BytesInBinaryMarshallable {
        Bytes<?> a, b, c, d, e, f, g, h;

        /**
         * Creates an instance with empty byte fields allocated for reading.
         */
        public WithBytes() {
            a = Bytes.elasticHeapByteBuffer(64);
            b = Bytes.elasticHeapByteBuffer(64);
            c = Bytes.allocateElasticDirect(64);
            d = Bytes.allocateElasticDirect(64);
            e = Bytes.elasticHeapByteBuffer(64);
            f = Bytes.elasticHeapByteBuffer(64);
            g = Bytes.allocateElasticDirect(64);
            h = Bytes.allocateElasticDirect(64);
        }

        /**
         * Creates an instance populated with string data converted to {@link Bytes}.
         */
        public WithBytes(String a, String b, String c, String d, String e, String f, String g, String h) {
            this.a = Bytes.fromDirect(a);
            this.b = Bytes.fromDirect(b);
            this.c = Bytes.fromDirect(c);
            this.d = Bytes.fromDirect(d);
            this.e = Bytes.from(e);
            this.f = Bytes.from(f);
            this.g = Bytes.from(g);
            this.h = Bytes.from(h);
        }

        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException {
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
