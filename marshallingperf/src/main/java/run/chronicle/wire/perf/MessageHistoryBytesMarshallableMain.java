package run.chronicle.wire.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.VanillaMessageHistory;

import static run.chronicle.wire.perf.BytesInBytesMarshallableMain.histoOut;

/*
.2.21.ea207
read: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.020 / 0.030  0.031 / 0.031  0.040 / 0.040  0.050 / 0.060  0.080 / 0.150  2.476 / 4.15 - 30.5
write: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.030 / 0.031  0.040 / 0.040  0.050 / 0.060  0.061 / 0.100  0.120 / 0.291  3.08 / 4.23 - 19.62

*/
public class MessageHistoryBytesMarshallableMain {

    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        SetTimeMessageHistory n = createMessageHistory();
        SetTimeMessageHistory n2 = new SetTimeMessageHistory();
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

        histoOut("read", MessageHistoryBytesMarshallableMain.class, readHist);
        histoOut("write", MessageHistoryBytesMarshallableMain.class, writeHist);
    }

    static SetTimeMessageHistory createMessageHistory() {
        SetTimeMessageHistory n = new SetTimeMessageHistory();
        n.addSource(1, 0xff);
        n.addSource(2, 0xfff);
        n.addTiming(10_000);
        n.addTiming(20_000);
        n.addTiming(30_000);
        n.addTiming(40_000);
        return n;
    }

    static class SetTimeMessageHistory extends VanillaMessageHistory {
        long nanoTime = 120962203520000L;

        @Override
        protected long nanoTime() {
            return nanoTime += 100;
        }
    }
}
