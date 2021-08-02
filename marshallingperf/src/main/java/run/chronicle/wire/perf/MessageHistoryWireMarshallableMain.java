package run.chronicle.wire.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

import static run.chronicle.wire.perf.BytesInBytesMarshallableMain.histoOut;
import static run.chronicle.wire.perf.MessageHistoryBytesMarshallableMain.createMessageHistory;

/*
.2.21.ea207
read: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.120 / 0.130  0.131 / 0.140  0.150 / 0.230  0.251 / 0.281  1.134 / 4.22  5.53 / 9.84 - 29.5
write: 50/90 97/99 99.7/99.9 99.97/99.99 99.997/99.999 99.9997/99.9999 - worst was 0.110 / 0.111  0.120 / 0.121  0.140 / 0.180  0.601 / 0.711  1.654 / 4.06  6.33 / 11.38 - 2,770

*/
public class MessageHistoryWireMarshallableMain {

    public static void main(String... args) {

        Histogram readHist = new Histogram();
        Histogram writeHist = new Histogram();

        MessageHistoryBytesMarshallableMain.SetTimeMessageHistory n = createMessageHistory();
        MessageHistoryBytesMarshallableMain.SetTimeMessageHistory n2 = new MessageHistoryBytesMarshallableMain.SetTimeMessageHistory();
        Bytes bytes = Bytes.allocateElasticDirect(128);
        Wire bw = WireType.BINARY.apply(bytes);

        for (int i = -20_000; i < 100_000_000; i++) {
            bytes.clear();
            long start = System.nanoTime();
            n.writeMarshallable(bw);
            long end = System.nanoTime();
            writeHist.sample(end - start);
            start = System.nanoTime();
            n2.readMarshallable(bw);
            end = System.nanoTime();
            readHist.sample(end - start);
            if (i == 0) {
                readHist.reset();
                writeHist.reset();
            }
            if (i >= -1000)
                Thread.yield();
        }

        histoOut("read", MessageHistoryWireMarshallableMain.class, readHist);
        histoOut("write", MessageHistoryWireMarshallableMain.class, writeHist);
    }
}
