/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static net.openhft.chronicle.wire.Marshallable.fromString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// see also UnsafeTextBytesTest
// Class to test the serialization and deserialization of double values.
public class DoubleTest extends WireTestCommon {

    @BeforeEach
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip double serialisation tests");
    }

    /**
     * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/299 Fixed case where a serialisable 'double' value sometimes has a trailing zero.
     */
    // Test the serialisation format of two double values without trailing zeros.
    @Test
    @DisplayName("Serialises two doubles without trailing zeros")
    public void testParsingForTwoDoubles() {
        CLASS_ALIASES.addAlias(TwoDoubleDto.class);

        // Expected serialized format
        final String EXPECTED = "!TwoDoubleDto {\n" +
                "  price: 43298.21,\n" +
                "  qty: 0.2886\n" +
                "}\n";
        final TwoDoubleDto twoDoubleDto = fromString(TwoDoubleDto.class, EXPECTED);

        Assertions.assertEquals(EXPECTED, twoDoubleDto.toString(),
                "two double dto should format without trailing zeros");
        assertEquals(43298.21, twoDoubleDto.price, 0.0,
                "two double dto price should parse correctly");
        assertEquals(0.2886, twoDoubleDto.qty, 0.0,
                "two double dto qty should parse correctly");
    }

    // Test the serialization of many double values ensuring no trailing zeros.
    @Test
    @DisplayName("Serialises many doubles without trailing zeros")
    public void testManyDoubles() {
        // Create an elastic buffer for serialization
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Iterate over a range of double values and check serialization format
        for (double aDouble = -1; aDouble < 1; aDouble += 0.00001) {
            bytes.clear();
            aDouble = Maths.round6(aDouble);
            bytes.append(aDouble);
            double d2 = bytes.parseDouble();
            assertEquals(aDouble, d2, Math.ulp(aDouble),
                    "parsed double should match rounded value, input=" + aDouble);

            // Ensure no trailing zeros
            final String message = bytes.toString();
            assertFalse(message.endsWith("0"),
                    "serialised double should not end with trailing zero for input=" + aDouble
                            + ", actual=" + message);
        }
        bytes.releaseLast();
    }

    // DTO representing two double values.
    private static class TwoDoubleDto extends SelfDescribingMarshallable {
        double price;
        double qty;
    }
}
