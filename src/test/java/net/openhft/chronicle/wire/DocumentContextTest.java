package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DocumentContextTest {
    @Test
    public void multiMessageText() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap()).useBinaryDocuments();
        Bytes<?> bytes = doTest(wire);
        bytes.readSkip(4);
        assertEquals("one: 1\n" +
                "two: 2\n" +
                "three: 3\n", bytes.toString());
    }

    @Test
    public void multiMessageBinary() {
        BinaryWire wire = new BinaryWire(new HexDumpBytes());
        Bytes<?> bytes = doTest(wire);
        assertEquals("" +
                "14 00 00 00                                     # msg-length\n" +
                "b9 03 6f 6e 65 01                               # one\n" +
                "b9 03 74 77 6f 02                               # two\n" +
                "b9 05 74 68 72 65 65 03                         # three\n", bytes.toHexString());
    }

    @NotNull
    private Bytes<?> doTest(Wire wire) {
        wire.acquireWritingDocument(false).wire().writeEventName("one").int16(1);
        wire.acquireWritingDocument(false).wire().writeEventName("two").int16(2);
        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            dc.wire().writeEventName("three").int16(3);
            dc.close();
            dc.close();
        }
        return wire.bytes();
    }
}
