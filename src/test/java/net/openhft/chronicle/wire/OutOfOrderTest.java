/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class OutOfOrderTest extends WireTestCommon {
    // Define JSON snippets to be used in tests
    private static final String start = "{ \"a\": 1, ";
    private static final String records = "\"records\":[{\"id\":1}], ";
    private static final String missing = "\"missing\": 111, ";
    private static final String end = "\"z\": 99 }";

    @Test
    public void outOfOrder() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Test JSON with just the start and end
        doTest(start + end, "{\"a\":1,\"b\":null,\"records\":null,\"z\":99}");
        // Test JSON with all segments included
        doTest(start + missing + records + end, "{\"a\":1,\"b\":null,\"records\":[ {\"id\":1} ],\"z\":99}");
    }

    private void doTest(String input, String expected) {
        // Convert the input string to bytes
        Bytes<?> from = Bytes.from(input);
        // Create a JSONWire object from the bytes
        JSONWire wire = new JSONWire(from);
        // Deserialize the input into an OOOT object
        OOOT ooot = wire.getValueIn().object(OOOT.class);
        from.releaseLast();  // Release the byte buffer

        // Serialize the OOOT object back into a new JSONWire
        JSONWire wire2 = new JSONWire(Bytes.allocateElasticOnHeap(64));
        wire2.getValueOut().object(ooot);
        // Assert the serialized result
        assertEquals(expected, wire2.toString());
    }

    // Helper class with various fields for testing
    private static class OOOT extends SelfDescribingMarshallable {
        int a;
        String b;
        List<OOOT2> records;
        int z;
    }

    // Nested helper class
    private static class OOOT2 extends SelfDescribingMarshallable {
        int id;
    }
}
