/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VanillaMessageHistoryTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Test to check the equality and hashcode of a VanillaMessageHistory object
    @Test
    @DisplayName("Serialises and compares message history state")
    public void equalsHashCode() {

        // Create and initialize a VanillaMessageHistory object
        VanillaMessageHistory vmh = new VanillaMessageHistory();
        vmh.addSourceDetails(true);
        vmh.useBytesMarshallable(false);
        vmh.addSource(1, 128);
        vmh.addTiming(12121212);

        // Create a class lookup for aliasing
        final ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES.wrap();
        classLookup.addAlias(VanillaMessageHistory.class, "VMH");

        // Initialize a BinaryWire object with HexDumpBytes
        BinaryWire wire = new BinaryWire(new HexDumpBytes());
        wire.classLookup(classLookup);

        // Write the VanillaMessageHistory object to the wire
        wire.write("vmh").object(vmh);

        // Assert the wire's content matches the expected hex format
        String expectedHex = "c3 76 6d 68                                     # vmh:\n" +
                        "b6 03 56 4d 48                                  # VMH\n" +
                        "81 33 00                                        # VanillaMessageHistory\n" +
                        "c7 73 6f 75 72 63 65 73                         # sources:\n" +
                        "82 0b 00 00 00                                  # sequence\n" +
                        "                                                # source id & index\n" +
                        "a1 01 af 80 00 00 00 00 00 00 00                # 1\n" +
                        "c7 74 69 6d 69 6e 67 73                         # timings:\n" +
                        "82 0e 00 00 00                                  # sequence\n" +
                        "                                                # timing in nanos\n" +
                        "a6 7c f4 b8 00                                  # 12121212\n" +
                        "a7 timestamp\n";
        String actualHex = wire.bytes().toHexString().replaceAll("\na7.*\n", "\na7 timestamp\n");
        assertEquals(expectedHex, actualHex, "Expected hex dump for serialised VMH");

        // Create two new VanillaMessageHistory objects for comparison
        VanillaMessageHistory vmh2 = new VanillaMessageHistory();
        vmh2.useBytesMarshallable(false);
        VanillaMessageHistory vmh3 = new VanillaMessageHistory();
        vmh3.useBytesMarshallable(false);

        // Check that the hash codes of the two new objects are equal
        assertEquals(vmh3.hashCode(), vmh2.hashCode(),
                "Expected new instances to have matching hash codes");

        // Read back the VanillaMessageHistory object from the wire into vmh2
        Object o = wire.read("vmh").object(vmh2, VanillaMessageHistory.class);
        assertNotNull(o, "Expected VMH object read from wire");

        // Add the last timing to the original VanillaMessageHistory (which gets added on read)
        vmh.addTiming(vmh2.timing(1));
        vmh2.addSourceDetails(true);

        // Assert the two VanillaMessageHistory objects are equal in content and hash code
        assertEquals(vmh.toString(), vmh2.toString(),
                "Expected VMH string form after read");

        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip equality check");
        assertEquals(vmh, vmh2, "Expected VMH equality after read and timing update");
        assertEquals(vmh.hashCode(), vmh2.hashCode(),
                "Expected VMH hash codes after read");
    }
}
