/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireMarshallerTest extends WireTestCommon {

    @Test
    public void usesBinary() {
        ClassAliasPool.CLASS_ALIASES.addAlias(WMTwoFields.class);
        // language=YAML
        String text = "!WMTwoFields {\n" +
                "  id: shelf.script.door,\n" +
                "  ts: 2019-11-17T12:56:42.108971\n" +
                "}\n";
        WMTwoFields tf = Marshallable.fromString(text);
        assert tf != null;
        assertEquals(text, tf.toString());
        HexDumpBytes bytes = new HexDumpBytes();
        Wire w = new BinaryWire(bytes);
        w.write("").object(WMTwoFields.class, tf);
        WMTwoFields tf2 = w.read().object(WMTwoFields.class);
        assertEquals(text, tf2.toString());
        assertEquals("" +
                "c0                                              # :\n" +
                "82 14 00 00 00                                  # WMTwoFields\n" +
                "   c2 69 64                                        # id:\n" +
                "   a6 d2 02 96 49                                  # 1234567890\n" +
                "   c2 74 73                                        # ts:\n" +
                "   a7 2b 20 d2 5c 8a 97 05 00                      # 1573995402108971\n", bytes.toHexString());
        bytes.releaseLast();
    }
}