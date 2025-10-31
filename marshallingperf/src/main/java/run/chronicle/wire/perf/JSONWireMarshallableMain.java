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
package run.chronicle.wire.perf;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.testframework.GcControls;
import net.openhft.chronicle.wire.*;

import static run.chronicle.wire.perf.BytesInBytesMarshallableMain.histoOut;

/**
 * Benchmarks the performance of marshalling and unmarshalling objects using
 * {@link JSONWire}. The {@link Example} class exercises several primitive and
 * string fields.
 */
public class JSONWireMarshallableMain {

    /**
     * Executes the JSON wire benchmark.
     *
     * <p>Iterations with a negative index warm the JVM. After warm up the
     * program measures the time to write and read an {@link Example} instance.
     * Results are printed as latency histograms.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        Example n = new Example(1, System.currentTimeMillis(), 1.2345, false, "hello, text!");

        Example n2 = new Example();
        Wire wire = WireType.JSON.apply(Bytes.allocateElasticDirect(128));

        try (AffinityLock lock = AffinityLock.acquireLock()) {
            // Warm up with negative iterations then record timings
            for (int i = -100_000; i < 50_000_000; i++) {
                wire.clear();
                long start = System.nanoTime();
                n.writeMarshallable(wire);
                long end = System.nanoTime();
                writeHist.sample(end - start);
                start = System.nanoTime();
                n2.readMarshallable(wire);
                end = System.nanoTime();
                readHist.sample(end - start);
                if (i == 0) {
                    System.out.println("Warmup complete, awaiting GC");
                    readHist.reset();
                    writeHist.reset();
                    GcControls.waitForGcCycle();
                    System.out.println("GC complete, starting benchmark");
                }
            }
        }

        System.out.println("Benchmark complete, writing results");

        histoOut("read", JSONWireMarshallableMain.class, readHist);
        histoOut("write", JSONWireMarshallableMain.class, writeHist);
    }

    /**
     * Simple data holder used to exercise JSON serialisation of various field types.
     */
    static class Example extends SelfDescribingMarshallable {
        int smallInt = 0;
        long longInt = 0;
        double price = 0;
        boolean flag = false;
        String text;

        /**
         * Creates an example populated with provided values.
         */
        Example(int smallInt, long longInt, double price, boolean flag, String text) {
            this.smallInt = smallInt;
            this.longInt = longInt;
            this.price = price;
            this.flag = flag;
            this.text = text;
        }

        /**
         * Creates an empty instance for reading into.
         */
        public Example() {
        }

        @Override
        public void writeMarshallable(WireOut wire) throws InvalidMarshallableException {
            wire.write("price").writeDouble(price);
            wire.write("flag").writeBoolean(flag);
            wire.write("text").writeString(text);
            wire.write("smallInt").writeInt(smallInt);
            wire.write("longInt").writeLong(longInt);
        }

        @Override
        public void readMarshallable(WireIn wire) throws IORuntimeException, InvalidMarshallableException {
            price = wire.read("price").readDouble();
            flag = wire.read("flag").readBoolean();
            text = wire.read("text").readString();
            smallInt = wire.read("smallInt").readInt();
            longInt = wire.read("longInt").readLong();
        }
    }
}
