/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VanillaMessageHistoryTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Test to check the equality and hashcode of a VanillaMessageHistory object
    @Test
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
        assertEquals("" +
                        "c3 76 6d 68                                     # vmh:\n" +
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
                        "a7 timestamp\n",
                wire.bytes().toHexString().replaceAll("\na7.*\n", "\na7 timestamp\n"));

        // Create two new VanillaMessageHistory objects for comparison
        VanillaMessageHistory vmh2 = new VanillaMessageHistory();
        vmh2.useBytesMarshallable(false);
        VanillaMessageHistory vmh3 = new VanillaMessageHistory();
        vmh3.useBytesMarshallable(false);

        // Check that the hash codes of the two new objects are equal
        assertEquals(vmh3.hashCode(),
                vmh2.hashCode());

        // Read back the VanillaMessageHistory object from the wire into vmh2
        Object o = wire.read("vmh").object(vmh2, VanillaMessageHistory.class);
        assertNotNull(o);

        // Add the last timing to the original VanillaMessageHistory (which gets added on read)
        vmh.addTiming(vmh2.timing(1));
        vmh2.addSourceDetails(true);

        // Assert the two VanillaMessageHistory objects are equal in content and hash code
        assertEquals(vmh.toString(), vmh2.toString());
        assertEquals(vmh, vmh2);
        assertEquals(vmh.hashCode(),
                vmh2.hashCode());
    }
}
