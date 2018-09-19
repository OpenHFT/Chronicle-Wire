package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireParserTest {

    @Test
    public void noOpReadOne() {
        BinaryWire wire = new BinaryWire(Bytes.elasticHeapByteBuffer(128));
        wire.bytes().writeUtf8("Hello world");

        WireParser.skipReadable(-1, wire);
        assertEquals(0, wire.bytes().readRemaining());
    }
}