/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class OutOfOrderTest extends WireTestCommon {
    // Define JSON snippets to be used in tests
    private static final String start = "{ \"a\": 1, ";
    private static final String records = "\"records\":[{\"id\":1}], ";
    private static final String missing = "\"missing\": 111, ";
    private static final String end = "\"z\": 99 }";

    @Test
    @DisplayName("Out-of-order JSON fields round-trip correctly")
    public void outOfOrder() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for out-of-order JSON test");

        // Test JSON with just the start and end
        assertEquals("{\"a\":1,\"b\":null,\"records\":null,\"z\":99}", doTest(start + end),
                "out-of-order minimal JSON should preserve defaults");
        // Test JSON with all segments included
        assertEquals("{\"a\":1,\"b\":null,\"records\":[ {\"id\":1} ],\"z\":99}", doTest(start + missing + records + end),
                "out-of-order full JSON should preserve records list");
    }

    private String doTest(String input) {
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
        return wire2.toString();
    }

    // Helper class with various fields for testing
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class OOOT extends SelfDescribingMarshallable {
        int a;
        String b;
        List<OOOT2> records;
        int z;
    }

    // Nested helper class
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class OOOT2 extends SelfDescribingMarshallable {
        int id;
    }
}
