/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises BinaryWire HexDumpBytes output when TwoArrays round-trips array capacity, used counts, and element values.
 */
public class TwoArraysTest extends WireTestCommon {
    @Test
    @DisplayName("TwoArrays serialises and deserialises via BinaryWire")
    public void testTwoArrays() {
        // Ignore exceptions with specific error message
        ignoreException("BytesMarshallable found in field which is not matching exactly");

        // Create a new HexDumpBytes which will be used to serialise the TwoArrays object
        Bytes<?> bytes = new HexDumpBytes();

        // Create a BinaryWire instance for serialisation and deserialisation
        Wire wire = new BinaryWire(bytes);

        // Create an instance of TwoArrays
        TwoArrays ta = new TwoArrays(4, 8);

        // Serialise the TwoArrays object
        ta.writeMarshallable(wire);
        assertEquals("   c2 69 61                                        # ia:\n" +
                        "   82 20 00 00 00                                  # BinaryIntArrayReference\n" +
                        "                                                # BinaryIntArrayReference\n" +
                        "   04 00 00 00 00 00 00 00                         # capacity\n" +
                        "   00 00 00 00 00 00 00 00                         # used\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                        "   c2 6c 61                                        # la:\n" +
                        "   82 50 00 00 00                                  # BinaryLongArrayReference\n" +
                        "                                                # BinaryLongArrayReference\n" +
                        "   08 00 00 00 00 00 00 00                         # capacity\n" +
                        "   00 00 00 00 00 00 00 00                         # used\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n",
                bytes.toHexString(),
                "Initial serialised form should match the expected hex output");

        TwoArrays ta2 = new TwoArrays(0, 0);

        // Deserialise the TwoArrays object
        ta2.readMarshallable(wire);

        // Assertions to validate deserialisation results
        assertEquals(4, ta2.ia.getCapacity(), "Int array capacity should match the initial value");
        assertEquals(8, ta2.la.getCapacity(), "Long array capacity should match the initial value");

        // Modify the values in the deserialised TwoArrays instance
        ta2.ia.setMaxUsed(1);
        ta2.ia.setValueAt(0, 11);
        ta2.la.setMaxUsed(2);
        ta2.la.setValueAt(0, 111);
        ta2.la.setValueAt(1, 222);

        // Serialise the modified TwoArrays object
        Bytes<?> bytes2 = new HexDumpBytes();
        Wire wire2 = new BinaryWire(bytes2);
        ta2.writeMarshallable(wire2);
        assertEquals("   c2 69 61                                        # ia:\n" +
                        "   82 20 00 00 00                                  # BinaryIntArrayReference\n" +
                        "   04 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 # BinaryIntArrayReference\n" +
                        "   0b 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # la:\n" +
                        "   c2 6c 61 82 50 00 00 00                         # BinaryLongArrayReference\n" +
                        "   08 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 # BinaryLongArrayReference\n" +
                        "   6f 00 00 00 00 00 00 00 de 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n",
                bytes2.toHexString(),
                "Modified serialised form should match the expected hex output");

        bytes.readPosition(0);

        // Deserialise the modified TwoArrays object
        TwoArrays ta3 = new TwoArrays(0, 0);
        ta3.readMarshallable(wire);

        // Assertions to validate deserialisation results of the modified TwoArrays instance
        assertEquals(4, ta3.ia.getCapacity(), "Int array capacity should round-trip");
        assertEquals(1, ta3.ia.getUsed(), "Int array used count should round-trip");
        assertEquals(11, ta3.ia.getValueAt(0), "Int array value at index 0 should round-trip");
        assertEquals(8, ta3.la.getCapacity(), "Long array capacity should round-trip");
        assertEquals(2, ta3.la.getUsed(), "Long array used count should round-trip");
        assertEquals(111, ta3.la.getValueAt(0), "Long array value at index 0 should round-trip");
        assertEquals(222, ta3.la.getValueAt(1), "Long array value at index 1 should round-trip");

        // Close resources and release memory
        ta.close();
        ta2.close();
        ta3.close();
        bytes2.releaseLast();
        bytes.releaseLast();
    }
}
