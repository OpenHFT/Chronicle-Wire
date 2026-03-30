/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.IdentifierLongConverter.*;
import static org.junit.jupiter.api.Assertions.*;

class IdentifierLongConverterTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Defining constants for the test class
    private static final String MAX_SMALL_POSITIVE_STR = "^^^^^^^^^^";

    // Test the parsing functionality for the minimum values
    @Test
    void parseMin() {
        assertEquals(0, INSTANCE.parse(""));
        assertEquals(0, INSTANCE.parse("000", 1, 2));
    }

    // Test parsing functionality for the maximum small positive value
    @Test
    void parseMaxSmallPositive() {
        assertEquals(MAX_SMALL_ID, INSTANCE.parse(MAX_SMALL_POSITIVE_STR));
    }

    // Test parsing functionality for one more than the max small positive value
    @Test
    void parseMaxSmallPositivePlus1() {
        assertEquals(MAX_SMALL_ID + 1, INSTANCE.parse(MIN_DATE));
    }

    // Test the string representation for zero
    @Test
    void asString0() {
        assertEquals("", INSTANCE.asString(0));
    }

    // Test the string representation for the max small value
    @Test
    void asStringMaxSmall() {
        assertEquals(MAX_SMALL_POSITIVE_STR, INSTANCE.asString(MAX_SMALL_ID));
    }

    // Test the string representation for one more than the max small value
    @Test
    void asStringMaxSmallPlus1() {
        assertEquals(MIN_DATE, INSTANCE.asString(MAX_SMALL_ID + 1));
    }

    // Test the string representation for the maximum DateTime value
    @Test
    void asStringMaxDateTime() {
        assertEquals(MAX_DATE, INSTANCE.asString(Long.MAX_VALUE));
    }

    // Test using the TextWire format with safe characters
    @Test
    void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    // Test using the YamlWire format with safe characters
    @Test
    void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        allSafeChars(wire);
    }

    // Helper function to test all safe characters for both TextWire and YamlWire formats
    private void allSafeChars(Wire wire) {
        final LongConverter converter = IdentifierLongConverter.INSTANCE;

        // Loop through the first 32 numbers to validate the conversion logic
        for (long i = 0; i < 32; i++) {
            wire.clear();

            // Write long values to the wire with the provided converter
            wire.write("a").writeLong(converter, i);

            // Write sequence values to the wire with the provided converter
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });

            // Assert that the written values match the expected ones
            assertEquals(i, wire.read("a").readLong(converter), wire.toString());
            wire.read("b").sequence(i, (i2, v) -> {
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
