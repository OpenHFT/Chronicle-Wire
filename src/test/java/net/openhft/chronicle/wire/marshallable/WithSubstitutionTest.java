/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test class for functionality related to substitutions in wire operations.
 * Extends WireTestCommon to utilize utilities related to wire tests.
 */
class WithSubstitutionTest extends WireTestCommon {

    /**
     * Tests the behavior of substitutions in wire deserialization.
     * Expects certain substitution-related exceptions and checks the deserialization behavior
     * when substitutions are present.
     */
    @Test
    @DisplayName("Substitutions fall back to defaults in YAML")
    void subs() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for substitution test");

        // Expect exceptions related to invalid number substitutions
        expectException("Text parser cannot read ${num} as a number; treating as 0");
        expectException("Text parser cannot read ${num2} as a number; treating as 0");
        expectException("Text parser cannot read ${d} as a number; treating as 0");
        expectException("Text parser cannot read ${d2} as a number; treating as 0");
        expectException("Found an unsubstituted ${} as ${text");

        // Add alias for the WSDTO class to handle its deserialization
        ClassAliasPool.CLASS_ALIASES.addAlias(WSDTO.class);

        // Deserialize a list of WSDTO objects with substitutions from a string representation
        List<WSDTO> wsdtos = Marshallable.fromString(
                "[\n" +
                        "  !WSDTO {\n" +
                        "    num: ${num},\n" +
                        "    d: ${d}\n" +
                        "    text: ${text}\n" +
                        "  },\n" +
                        "  !WSDTO {\n" +
                        "    num: ${num2},\n" +
                        "    text: ${text2}\n" +
                        "    d: ${d2}\n" +
                        "  }\n" +
                        "]\n");

        // Assert the deserialized list matches the expected output
        assertEquals("[!WSDTO {\n" +
                "  num: 0,\n" +
                "  d: 0.0,\n" +
                "  text: \"${text}\"\n" +
                "}\n" +
                ", !WSDTO {\n" +
                "  num: 0,\n" +
                "  d: 0.0,\n" +
                "  text: \"${text2}\"\n" +
                "}\n" +
                "]", wsdtos.toString(),
                "Substituted placeholders should fall back to default values");
        assertEquals(2, wsdtos.size(), "Substitution test should return two DTOs");
        assertEquals(0, wsdtos.get(0).num, "First substitution num should be zero");
        assertEquals(0.0, wsdtos.get(0).d, 0.0, "First substitution double should be zero");
        assertEquals("${text}", wsdtos.get(0).text, "First substitution text should be literal");
        assertEquals(0, wsdtos.get(1).num, "Second substitution num should be zero");
        assertEquals(0.0, wsdtos.get(1).d, 0.0, "Second substitution double should be zero");
        assertEquals("${text2}", wsdtos.get(1).text, "Second substitution text should be literal");
    }

    /**
     * Data Transfer Object (DTO) representing the wire structure with potential substitutions.
     */
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class WSDTO extends SelfDescribingMarshallable {
        int num;    // Integer field that can have substitutions
        double d;   // Double field that can have substitutions
        String text; // String field that can have substitutions
    }
}
