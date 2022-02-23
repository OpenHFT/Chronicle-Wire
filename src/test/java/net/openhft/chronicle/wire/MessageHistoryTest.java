package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.After;
import org.junit.Test;

import static net.openhft.chronicle.bytes.MethodReader.MESSAGE_HISTORY_METHOD_ID;
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
        VanillaMessageHistory.USE_BYTES_MARSHALLABLE = true;
        VanillaMessageHistory history = new SetTimeMessageHistory();
        history.addSource(1, 0xff);
        history.addSource(2, 0xfff);
        history.addTiming(10_000);
        history.addTiming(20_000);
        BinaryWire bw = new BinaryWire(Bytes.elasticHeapByteBuffer());
        history.writeMarshallable(bw);
        VanillaMessageHistory history2 = new SetTimeMessageHistory();
        history2.readMarshallable(bw);
        assertEquals("VanillaMessageHistory{" +
                "sources: [1=0xff,2=0xfff] " +
                "timings: [10000,20000,120962203520100] " +
                "addSourceDetails=false}", history2.toString());

        bw.bytes().readPosition(0);
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
        bw.parent(sc);
        history2.readMarshallable(bw);
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
        bw.writeEventName(MethodReader.HISTORY).marshallable(history);
        assertEquals("" +
                        "b9 07 68 69 73 74 6f 72 79                      # history\n" +
                        "82 3d 00 00 00                                  # SetTimeMessageHistory\n" +
                        "c7 73 6f 75 72 63 65 73                         # sources\n" +
                        "82 14 00 00 00                                  # sequence\n" +
                        "01 af ff 00 00 00 00 00 00 00                   # source id & index\n" +
                        "02 af ff 0f 00 00 00 00 00 00                   # source id & index\n" +
                        "c7 74 69 6d 69 6e 67 73                         # timings\n" +
                        "82 0f 00 00 00                                  # sequence\n" +
                        "                                                # timing in nanos\n" +
                        "a5 10 27                                        # 10000\n" +
                        "                                                # timing in nanos\n" +
                        "a5 20 4e                                        # 20000\n" +
                        "a7 64 0c 2c b5 03 6e 00 00                      # 120962203520100\n",
                bw.bytes().toHexString());
        bw.bytes().releaseLast();

        assertEquals("VanillaMessageHistory{sources: [1=0xff,2=0xfff] timings: [10000,20000] addSourceDetails=true}",
                history.toString());
        assertEquals(2, history.sources());
        assertEquals(2, history.timings());
    }

    @After
    public void tearDown() {
        VanillaMessageHistory.USE_BYTES_MARSHALLABLE = false;
    }

    @Test
    public void testReadMarshallable() {
        SetTimeMessageHistory vmh = new SetTimeMessageHistory();
        vmh.addSource(1, 2);
        vmh.addTiming(1111);
        vmh.addTiming(2222);

        HexDumpBytes bytes = new HexDumpBytes();
        Wire wire = new BinaryWire(bytes);
        VanillaMessageHistory.USE_BYTES_MARSHALLABLE = false;
        wire.write(MethodReader.HISTORY).object(SetTimeMessageHistory.class, vmh);

        vmh.nanoTime = 120962203520000L;
        VanillaMessageHistory.USE_BYTES_MARSHALLABLE = true;
        wire.writeEventId(MESSAGE_HISTORY_METHOD_ID).object(SetTimeMessageHistory.class, vmh);

        assertEquals("" +
                        "c7 68 69 73 74 6f 72 79                         # history\n" +
                        "82 33 00 00 00                                  # SetTimeMessageHistory\n" +
                        "c7 73 6f 75 72 63 65 73                         # sources\n" +
                        "82 0a 00 00 00                                  # sequence\n" +
                        "01 af 02 00 00 00 00 00 00 00                   # source id & index\n" +
                        "c7 74 69 6d 69 6e 67 73                         # timings\n" +
                        "82 0f 00 00 00                                  # sequence\n" +
                        "                                                # timing in nanos\n" +
                        "a5 57 04                                        # 1111\n" +
                        "                                                # timing in nanos\n" +
                        "a5 ae 08                                        # 2222\n" +
                        "a7 64 0c 2c b5 03 6e 00 00 ba 80 00             # 120962203520100\n" +
                        "82 27 00 00 00 86                               # SetTimeMessageHistory\n" +
                        "01 01 00 00 00 02 00 00 00 00 00 00 00          # sources\n" +
                        "03 57 04 00 00 00 00 00 00 ae 08 00 00 00 00 00 # timings\n" +
                        "00 64 0c 2c b5 03 6e 00 00\n",
                bytes.toHexString());
        vmh.addTiming(120962203520100L);

        VanillaMessageHistory vmh2 = new VanillaMessageHistory();
        wire.read().object(vmh2, VanillaMessageHistory.class);
        assertEquals(vmh.toString(), vmh2.toString());

        VanillaMessageHistory vmh3 = new VanillaMessageHistory();
        wire.read().object(vmh3, VanillaMessageHistory.class);
        assertEquals(vmh.toString(), vmh3.toString());
    }

    static class SetTimeMessageHistory extends VanillaMessageHistory {
        long nanoTime = 120962203520000L;

        @Override
        protected long nanoTime() {
            return nanoTime += 100;
        }
    }
}
