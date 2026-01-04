/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base32LongConverterTest extends WireTestCommon {

    // A test to check the parsing functionality of Base32LongConverter.
    @Test
    @DisplayName("Parses and formats Base32 tokens correctly")
    void parse() {
        LongConverter bic = new Base32LongConverter();

        // Iterate through predefined string values to check the conversion consistency
        // in both original and lower case forms.
        for (String s : ",O,A,L,ZZ,QQ,ABCDEGHIJKLM,5OPQRSTVWXYZ,JZZZZZZZZZZZ".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)),
                    "Expected Base32 round-trip for input=" + s);
            assertEquals(s, bic.asString(bic.parse(s.toLowerCase())),
                    "Expected Base32 case-insensitive round-trip for input=" + s);
        }
    }

    // A test to check the character safety in TextWire.
    @Test
    @DisplayName("Parses Base32 subsequences without error safely")
    void parseSubsequence() {
        LongConverter c = Base32LongConverter.INSTANCE;
        String s = ",O,A,L,ZZ,QQ,ABCDEGHIJKLM,5OPQRSTVWXYZ,JZZZZZZZZZZZ,";
        int comparisons = 9;
        assertDoesNotThrow(() -> subStringParseLoop(s, c, comparisons),
                "Expected substring parse loop to complete");
    }

    @Test
    @DisplayName("Rejects overly long Base32 input strings")
    void parseLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                Base32LongConverter.INSTANCE.parse(getClass().getCanonicalName()),
                "Expected Base32 parse to reject long input");
    }

    @Test
    @DisplayName("Rejects invalid Base32 substring bounds cases")
    void parseSubstringLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                Base32LongConverter.INSTANCE.parse("ABCD", 3, 0),
                "Expected Base32 parse to reject invalid substring bounds");
    }

    @Test
    @DisplayName("Allows safe Base32 characters in TextWire")
    void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        assertInstanceOf(TextWire.class, wire, "Expected TextWire instance for safe character checks");
        LongConverterTestSupport.allSafeChars(wire, Base32LongConverter.INSTANCE, 32 * 32L);
    }

    // A test to check the character safety in YamlWire.
    @Test
    @DisplayName("Allows safe Base32 characters in YamlWire")
    void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        assertInstanceOf(YamlWire.class, wire, "Expected YamlWire instance for safe character checks");
        LongConverterTestSupport.allSafeChars(wire, Base32LongConverter.INSTANCE, 32 * 32L);
    }
}
