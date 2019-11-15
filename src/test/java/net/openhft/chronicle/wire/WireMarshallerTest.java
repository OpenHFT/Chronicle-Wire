/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireMarshallerTest {

    @Test
    public void usesBinary() {
        String text = "!net.openhft.chronicle.wire.WireMarshallerTest$TwoFields {\n" +
                "  id: shelf.script.door,\n" +
                "  ts: 2019-11-17T12:56:42.108971\n" +
                "}\n";
        TwoFields tf = Marshallable.fromString(text);
        assertEquals(text, tf.toString());
        HexDumpBytes bytes = new HexDumpBytes();
        Wire w = new BinaryWire(bytes);
        w.write("").object(TwoFields.class, tf);
        TwoFields tf2 = w.read().object(TwoFields.class);
        assertEquals(text, tf2.toString());
        assertEquals("c0 82 14 00 00 00\n" +
                "   c2 69 64 a6 d2 02 96 49                         # id\n" +
                "   c2 74 73 a7 2b 20 d2 5c 8a 97 05 00             # ts\n", bytes.toHexString());
    }

    static class TwoFields extends AbstractMarshallable {
        @IntConversion(WordsIntConverter.class)
        int id;
        @LongConversion(MicroTimestampLongConverter.class)
        long ts;
    }

}