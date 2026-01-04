/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.IdentifierLongConverter.*;
import static org.junit.jupiter.api.Assertions.*;

class IdentifierLongConverterTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Defining constants for the test class
    private static final String MAX_SMALL_POSITIVE_STR = "^^^^^^^^^^";

    // Test the parsing functionality for the minimum values
    @Test
    @DisplayName("Parses empty and zero padded identifiers")
    void parseMin() {
        assertEquals(0, INSTANCE.parse(""),
                "empty string should parse to zero");
        assertEquals(0, INSTANCE.parse("000", 1, 2),
                "subrange of zeros should parse to zero");
    }

    // Test parsing functionality for the maximum small positive value
    @Test
    @DisplayName("Parses the maximum small positive identifier value")
    void parseMaxSmallPositive() {
        assertEquals(MAX_SMALL_ID,
                INSTANCE.parse(MAX_SMALL_POSITIVE_STR),
                "max small identifier should parse to MAX_SMALL_ID");
    }

    // Test parsing functionality for one more than the max small positive value
    @Test
    @DisplayName("Parses minimum date identifier as max small plus one")
    void parseMaxSmallPositivePlus1() {
        assertEquals(MAX_SMALL_ID + 1,
                INSTANCE.parse(MIN_DATE),
                "min date identifier should parse to MAX_SMALL_ID + 1");
    }

    // Test the string representation for zero
    @Test
    @DisplayName("Formats zero as empty identifier string")
    void asString0() {
        assertEquals("",
                INSTANCE.asString(0),
                "zero should format to empty identifier string");
    }

    // Test the string representation for the max small value
    @Test
    @DisplayName("Formats the maximum small identifier string")
    void asStringMaxSmall() {
        assertEquals(MAX_SMALL_POSITIVE_STR,
                INSTANCE.asString(MAX_SMALL_ID),
                "max small id should format to expected string");
    }

    // Test the string representation for one more than the max small value
    @Test
    @DisplayName("Formats the minimum date identifier string")
    void asStringMaxSmallPlus1() {
        assertEquals(MIN_DATE,
                INSTANCE.asString(MAX_SMALL_ID + 1),
                "max small id plus one should format to min date string");
    }

    // Test the string representation for the maximum DateTime value
    @Test
    @DisplayName("Formats the maximum date identifier string")
    void asStringMaxDateTime() {
        assertEquals(MAX_DATE,
                INSTANCE.asString(Long.MAX_VALUE),
                "max date should format to expected identifier string");
    }

    // Test using the TextWire format with safe characters
    @Test
    @DisplayName("Supports safe characters in text wire")
    void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        assertInstanceOf(TextWire.class, wire, "Safe character test should use TextWire");
        LongConverterTestSupport.allSafeChars(wire, IdentifierLongConverter.INSTANCE, 31);
    }

    // Test using the YamlWire format with safe characters
    @Test
    @DisplayName("Supports safe characters in yaml wire")
    void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        assertInstanceOf(YamlWire.class, wire, "Safe character test should use YamlWire");
        LongConverterTestSupport.allSafeChars(wire, IdentifierLongConverter.INSTANCE, 31);
    }
}
