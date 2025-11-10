//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * Test class for functionality related to substitutions in wire operations.
 * Extends WireTestCommon to utilize utilities related to wire tests.
 */
public class WithSubstitutionTest extends WireTestCommon {

    /**
     * Tests the behavior of substitutions in wire deserialization.
     * Expects certain substitution-related exceptions and checks the deserialization behavior
     * when substitutions are present.
     */
    @Test
    public void subs() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Expect exceptions related to invalid number substitutions
        expectException("Cannot read ${num} as a number, treating as 0");
        expectException("Cannot read ${num2} as a number, treating as 0");
        expectException("Cannot read ${d} as a number, treating as 0");
        expectException("Cannot read ${d2} as a number, treating as 0");
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
                "]", wsdtos.toString());
    }

    /**
     * Data Transfer Object (DTO) representing the wire structure with potential substitutions.
     */
    private static class WSDTO extends SelfDescribingMarshallable {
        int num;    // Integer field that can have substitutions
        double d;   // Double field that can have substitutions
        String text; // String field that can have substitutions
    }
}
