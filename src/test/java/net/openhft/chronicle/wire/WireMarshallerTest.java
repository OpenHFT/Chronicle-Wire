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
        // Add an alias for the WMTwoFields class.
        ClassAliasPool.CLASS_ALIASES.addAlias(WMTwoFields.class);

        // Define a string representation of the WMTwoFields object in YAML format.
        String text = "!WMTwoFields {\n" +
                "  id: shelf.script.door,\n" +
                "  ts: 2019-11-17T12:56:42.108971\n" +
                "}\n";

        // Parse the string representation to get the WMTwoFields object.
        WMTwoFields tf = Marshallable.fromString(text);

        // Check the parsed object is not null.
        assert tf != null;

        // Assert that the string representation of the parsed object matches the original text.
        assertEquals(text, tf.toString());

        // Create a HexDumpBytes object for examining binary content in hexadecimal format.
        HexDumpBytes bytes = new HexDumpBytes();

        // Initialize the BinaryWire for serialization.
        Wire w = new BinaryWire(bytes);

        // Serialize the WMTwoFields object to binary using Wire.
        w.write("").object(WMTwoFields.class, tf);

        // Deserialize the WMTwoFields object from binary using Wire.
        WMTwoFields tf2 = w.read().object(WMTwoFields.class);

        // Assert that the string representation of the deserialized object matches the original text.
        assertEquals(text, tf2.toString());

        // Assert that the serialized binary content matches the expected hexadecimal format.
        assertEquals("" +
                "c0                                              # :\n" +
                "82 14 00 00 00                                  # WMTwoFields\n" +
                "   c2 69 64                                        # id:\n" +
                "   a6 d2 02 96 49                                  # 1234567890\n" +
                "   c2 74 73                                        # ts:\n" +
                "   a7 2b 20 d2 5c 8a 97 05 00                      # 1573995402108971\n", bytes.toHexString());

        // Release the resources used by the HexDumpBytes object.
        bytes.releaseLast();
    }
}
