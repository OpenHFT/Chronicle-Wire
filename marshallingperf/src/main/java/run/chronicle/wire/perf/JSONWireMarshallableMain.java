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

public class JSONWireMarshallableMain {

    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        Example n = new Example(1, System.currentTimeMillis(), 1.2345, false, "hello, text!");

        Example n2 = new Example();
        Wire wire = WireType.JSON.apply(Bytes.allocateElasticDirect(128));

        try (AffinityLock lock = AffinityLock.acquireLock()) {
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

    static class Example extends SelfDescribingMarshallable {
        int smallInt = 0;
        long longInt = 0;
        double price = 0;
        boolean flag = false;
        String text;

        Example(int smallInt, long longInt, double price, boolean flag, String text) {
            this.smallInt = smallInt;
            this.longInt = longInt;
            this.price = price;
            this.flag = flag;
            this.text = text;
        }

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
