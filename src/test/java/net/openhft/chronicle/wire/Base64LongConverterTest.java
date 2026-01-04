/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class Base64LongConverterTest extends WireTestCommon {

    // Validate the parsing of Base64 encoded strings to long and vice versa
    @Test
    @DisplayName("Parses and formats Base64 tokens correctly")
    void parse() {
        // Obtain the singleton instance of Base64LongConverter
        LongConverter c = Base64LongConverter.INSTANCE;
        // Iterate through predefined strings, validate conversion from string to long and back to string
        for (String s : ",a,ab,abc,abcd,ab.de,123_56,1234567,12345678,123456789,z23456789,z234567890,O_________".split(",")) {
            long v = c.parse(s);
            assertEquals(s, c.asString(v), "Base64 round-trip should preserve input=" + s);
        }
    }

    // Validate string conversion of randomly generated long numbers
    @Test
    @DisplayName("Parses Base64 subsequences without error safely")
    void parseSubsequence() {
        LongConverter c = Base64LongConverter.INSTANCE;
        String s = ",a,ab,abc,abcd,ab.de,123_56,1234567,12345678,123456789,z23456789,z234567890,O_________,";
        int comparisons = 13;
        assertDoesNotThrow(() -> subStringParseLoop(s, c, comparisons),
                "substring parse loop should complete");
    }

    @Test
    @DisplayName("Rejects overly long Base64 input strings")
    void parseLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                Base64LongConverter.INSTANCE.parse(getClass().getCanonicalName()),
                "Base64 parse should reject long input");
    }

    @Test
    @DisplayName("Rejects invalid Base64 substring bounds cases")
    void parseSubstringLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                Base64LongConverter.INSTANCE.parse("abcd", 0, 5),
                "Base64 parse should reject invalid substring bounds");
    }

    @Test
    @DisplayName("Formats and parses random Base64 values")
    void asString() {
        // Obtain the singleton instance of Base64LongConverter
        LongConverter c = Base64LongConverter.INSTANCE;
        // Initialise a deterministic number generator for stable test coverage
        SplittableRandom rand = new SplittableRandom(0x5f3759df);

        // Validate the conversion of 128 randomly generated long numbers
        for (int i = 0; i < 128; i++) {
            long l = rand.nextLong();
            // Convert the long number to a Base64 encoded string
            String s = c.asString(l);
            // Assert conversion consistency by parsing it back and comparing with the original long number
            Assertions.assertEquals(l, c.parse(s),
                    "Base64 round-trip should preserve index=" + i + ", encoded=" + s);
        }
    }

    // Ensure safe character conversion using TextWire
    @Test
    @DisplayName("Allows safe Base64 characters in TextWire")
    void allSafeCharsTextWire() {
        // Create a TextWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        assertInstanceOf(TextWire.class, wire, "TextWire instance should exist for safe character checks");
        // Execute the generic safe character check
        LongConverterTestSupport.allSafeChars(wire, Base64LongConverter.INSTANCE, 64 * 64L);
    }

    // Ensure safe character conversion using YamlWire
    @Test
    @DisplayName("Allows safe Base64 characters in YamlWire")
    void allSafeCharsYamlWire() {
        // Create a YamlWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new YamlWire();
        assertInstanceOf(YamlWire.class, wire, "YamlWire instance should exist for safe character checks");
        // Execute the generic safe character check
        LongConverterTestSupport.allSafeChars(wire, Base64LongConverter.INSTANCE, 64 * 64L);
    }
}
