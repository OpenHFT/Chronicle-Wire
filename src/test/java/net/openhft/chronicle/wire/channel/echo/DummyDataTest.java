package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DummyDataTest {

    @Test
    public void readMarshallable() {
        DummyData dd = new DummyData()
                .timeNS(12345)
                .data(new byte[16]);
        final HexDumpBytes bytes = new HexDumpBytes();
        Wire wire = new BinaryWire(bytes);
        wire.getValueOut().object(DummyData.class, dd);
        assertEquals("" +
                        "81 1a 00 39 30 00 00 00 00 00 00 10 00 00 00 00 # DummyData\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00\n",
                bytes.toHexString());
        DummyData dd2 = new DummyData();
        wire.getValueIn().object(dd2, DummyData.class);
        assertEquals(dd.timeNS(), dd2.timeNS());
        assertArrayEquals(dd.data(), dd2.data());
    }
}