package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class MessageHistoryTest extends WireTestCommon {

    @Test
    public void checkHistoryGetClear() {
        MessageHistory mg = MessageHistory.get();
        assertNotNull(mg);
        MessageHistory.set(null);
        MessageHistory mg2 = MessageHistory.get();
        assertNotNull(mg2);
        assertNotSame(mg, mg2);
    }

    @Test
    public void checkDeepCopy() {
        VanillaMessageHistory history = new VanillaMessageHistory();
        history.addSource(1, 0xff);
        history.addSource(2, 0xfff);
        history.addTiming(10_000);
        history.addTiming(20_000);
        VanillaMessageHistory history2 = history.deepCopy();
        assertEquals(history, history2);
    }

    @Test
    public void checkHistoryMaxSizeException() {
        VanillaMessageHistory container1 = new VanillaMessageHistory();
        container1.addSourceDetails(true);
        VanillaMessageHistory container2 = new VanillaMessageHistory();
        container2.addSourceDetails(true);
        for (int i = 0; i < VanillaMessageHistory.MESSAGE_HISTORY_LENGTH / 2; i++) {
            Wires.copyTo(container1, container2);
            Wires.copyTo(container2, container1);
        }
        try {
            Wires.copyTo(container1, container2);
            fail();
        } catch (IllegalStateException e) {
            // all good
        }
    }

    @Test
    public void checkSerialiseBytes() {
        VanillaMessageHistory history = new SetTimeMessageHistory();
        history.addSource(1, 0xff);
        history.addSource(2, 0xfff);
        history.addTiming(10_000);
        history.addTiming(20_000);
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer();
        history.writeMarshallable(bytes);
        VanillaMessageHistory history2 = new SetTimeMessageHistory();
        history2.readMarshallable(bytes);
        assertEquals("VanillaMessageHistory{" +
                "sources: [1=0xff,2=0xfff] " +
                "timings: [10000,20000,120962203520100] " +
                "addSourceDetails=false}", history2.toString());

        bytes.readPosition(0);
        history2.addSourceDetails(true);
        SourceContext sc = new SourceContext() {
            @Override
            public int sourceId() {
                return 3;
            }
            @Override
            public long index() throws IORuntimeException {
                return 0xffff;
            }
        };
        history2.readMarshallable(bytes, sc);
        assertEquals("VanillaMessageHistory{" +
                "sources: [1=0xff,2=0xfff,3=0xffff] " +
                "timings: [10000,20000,120962203520100,120962203520100] " +
                "addSourceDetails=true}", history2.toString());
    }

    @Test
    public void checkToString() {
        VanillaMessageHistory history = new SetTimeMessageHistory();
        history.addSourceDetails(true);
        history.addSource(1, 0xff);
        history.addSource(2, 0xfff);
        history.addTiming(10_000);
        history.addTiming(20_000);
        assertEquals(2, history.sources());
        assertEquals(2, history.timings());

        BinaryWire bw = new BinaryWire(new HexDumpBytes());
        bw.writeEventName("history").marshallable(history);
        assertEquals("" +
                "b9 07 68 69 73 74 6f 72 79                      # history\n" +
                "82 3d 00 00 00                                  # SetTimeMessageHistory\n" +
                "c7 73 6f 75 72 63 65 73 82 14 00 00 00          # sources\n" +
                "01 af ff 00 00 00 00 00 00 00                   # source id & index\n" +
                "02 af ff 0f 00 00 00 00 00 00                   # source id & index\n" +
                "c7 74 69 6d 69 6e 67 73 82 0f 00 00 00          # timings\n" +
                "a5 10 27                                        # timing in nanos\n" +
                "a5 20 4e                                        # timing in nanos\n" +
                "a7 64 0c 2c b5 03 6e 00 00                      # 120962203520100\n", bw.bytes().toHexString());
        bw.bytes().releaseLast();

        assertEquals("VanillaMessageHistory{sources: [1=0xff,2=0xfff] timings: [10000,20000] addSourceDetails=true}",
                history.toString());
        assertEquals(2, history.sources());
        assertEquals(2, history.timings());
    }

    static class SetTimeMessageHistory extends VanillaMessageHistory {
        long nanoTime = 120962203520000L;

        @Override
        protected long nanoTime() {
            return nanoTime += 100;
        }
    }
}
