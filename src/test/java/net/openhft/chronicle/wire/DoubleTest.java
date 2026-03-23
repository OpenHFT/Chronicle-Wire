/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static net.openhft.chronicle.wire.Marshallable.fromString;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

// see also UnsafeTextBytesTest
// Class to test the serialization and deserialization of double values.
public class DoubleTest extends WireTestCommon {

    // DTO representing two double values.
    private static class TwoDoubleDto extends SelfDescribingMarshallable {
        double price;
        double qty;
    }

    @BeforeEach
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    /**
     * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/299 Fixed case where a serializable 'double' value sometimes has trailing zero
     */
     // Test the serialization format of two double values without trailing zeros.
    @Test
    public void testParsingForTwoDoubles() {
        CLASS_ALIASES.addAlias(TwoDoubleDto.class);

        // Expected serialized format
        final String EXPECTED = "!TwoDoubleDto {\n" +
                "  price: 43298.21,\n" +
                "  qty: 0.2886\n" +
                "}\n";
        final TwoDoubleDto twoDoubleDto = fromString(TwoDoubleDto.class, EXPECTED);

        assertEquals(EXPECTED, twoDoubleDto.toString());
    }

    // Test the serialization of many double values ensuring no trailing zeros.
    @Test
    public void testManyDoubles() {
        // Create an elastic buffer for serialization
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Iterate over a range of double values and check serialization format
        for (double aDouble = -1; aDouble < 1; aDouble += 0.00001) {
            bytes.clear();
            aDouble = Maths.round6(aDouble);
            bytes.append(aDouble);
            double d2 = bytes.parseDouble();
            assertEquals(aDouble, d2, Math.ulp(aDouble));

            // Ensure no trailing zeros
            final String message = bytes.toString();
            assertFalse(message.endsWith("0"), message + " has trailing 0");
        }
        bytes.releaseLast();
    }
}
