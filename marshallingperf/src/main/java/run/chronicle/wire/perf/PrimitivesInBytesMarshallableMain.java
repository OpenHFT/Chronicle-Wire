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
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

import static run.chronicle.wire.perf.BytesInBytesMarshallableMain.histoOut;

/**
 * Benchmarks the performance of serialising and deserialising an object
 * containing primitive fields using {@link net.openhft.chronicle.bytes.BytesMarshallable}.
 */
public class PrimitivesInBytesMarshallableMain {

    /**
     * Executes the primitive field benchmark.
     *
     * <p>A warm up is followed by measured iterations. Latency for each
     * serialisation and deserialisation is stored in histograms.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        WithPrimitives n = new WithPrimitives();

        WithPrimitives n2 = new WithPrimitives();
        Bytes<?> bytes = Bytes.elasticByteBuffer(256);

        // Use negative iterations for warm up
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
            // Reset histograms after warming up
            if (i == 0) {
                readHist.reset();
                writeHist.reset();
            }
            if (i >= -1000)
                Thread.yield();
        }

        histoOut("read", PrimitivesInBytesMarshallableMain.class, readHist);
        histoOut("write", PrimitivesInBytesMarshallableMain.class, writeHist);
    }

    /**
     * Object containing various primitive fields used for the benchmark.
     */
    static class WithPrimitives extends BytesInBinaryMarshallable {
        boolean a;
        byte b;
        char c;
        short d;
        int e;
        float f;
        long g;
        double h;

        /**
         * Constructs an empty instance for reading into.
         */
        public WithPrimitives() {
        }

        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException {
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
        public void writeMarshallable(BytesOut<?> bytes) {
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
