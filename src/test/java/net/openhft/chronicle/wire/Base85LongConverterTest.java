/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class Base85LongConverterTest extends WireTestCommon {

    // A sample string to test the parsing functionality.
    private static final CharSequence TEST_STRING = "world";
    private static final String PREFIX_HELLO = "hello";

    @Test
    @DisplayName("Parses Base85 leading zeros as zero")
    void parseLeadingZero() {
        LongConverter c = Base85LongConverter.INSTANCE;
        assertEquals(0L, c.parse("0"), "base85 single zero should decode to 0L");
        assertEquals(0L, c.parse("00"), "base85 two zeros should decode to 0L");
        assertEquals(0L, c.parse("000"), "base85 three zeros should decode to 0L");
        assertEquals(0L, c.parse("0000"), "base85 four zeros should decode to 0L");
        assertEquals(0L, c.parse("00000"), "base85 five zeros should decode to 0L");
        assertEquals(0L, c.parse("000000"), "base85 six zeros should decode to 0L");
        assertEquals(0L, c.parse("0000000"), "base85 seven zeros should decode to 0L");
        assertEquals(0L, c.parse("00000000"), "base85 eight zeros should decode to 0L");
        assertEquals(0L, c.parse("000000000"), "base85 nine zeros should decode to 0L");
        assertEquals(0L, c.parse("0000000000"), "base85 ten zeros should decode to 0L (maximum allowed)");
        assertThrows(IllegalArgumentException.class, () -> c.parse("00000000000"),
                "base85 parser should reject strings exceeding maximum length of 10 characters");
        assertEquals("", c.asString(0L), "base85 encoding of 0L should produce empty string");
    }

    @Test
    @DisplayName("Parses and appends Base85 tokens correctly")
    void parse() {
        // Obtain the singleton instance of Base85LongConverter
        LongConverter c = Base85LongConverter.INSTANCE;
        // Iterate through predefined strings, validate parsing and string reconstruction
        for (String s : ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0".split(",")) {
            // Parse the string into a long value
            long v = c.parse(s);
            // Convert the parsed long value back into a string and validate against the original string
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString(),
                    "base85 round-trip should hold for input=" + s);
        }
    }

    @Test
    @DisplayName("Parses Base85 subsequences without error safely")
    void parseSubsequence() {
        LongConverter c = Base85LongConverter.INSTANCE;
        String s = ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0,";
        int comparisons = 11;
        subStringParseLoop(s, c, comparisons);
        // subStringParseLoop performs assertions internally, test completion indicates success
    }

    @Test
    @DisplayName("Rejects overly long Base85 input strings")
    void parseLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                        Base85LongConverter.INSTANCE.parse(getClass().getCanonicalName()),
                "base85 parser should reject strings exceeding maximum encodable length");
    }

    @Test
    @DisplayName("Rejects invalid Base85 substring bounds cases")
    void parseSubstringLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                        Base85LongConverter.INSTANCE.parse("ABCD", -1, 3),
                "base85 parser should reject negative start indices in substring parsing");
    }

    @Test
    @DisplayName("Formats and parses Base85 values correctly")
    void asString() {
        // Obtain the singleton instance of Base85LongConverter
        LongConverter c = Base85LongConverter.INSTANCE;
        long sample = 0x1234_5678_9abc_def0L;
        assertEquals(sample, c.parse(c.asString(sample)),
                "base85 roundtrip conversion should preserve long value exactly");
        IntStream.range(0, 10_000_000)
                .parallel()
                .mapToLong(i -> ThreadLocalRandom.current().nextLong())
                .forEach(l -> {
                    String s = c.asString(l);
                    assertEquals(l, c.parse(s),
                            "base85 roundtrip conversion should preserve random long value: " + s);
                });
    }

    // Validate the append operation for a known input string
    @Test
    @DisplayName("Appends Base85 text for known input")
    void testAppend() {
        LongConverterTestSupport.assertAppend(TEST_STRING, Base85LongConverter.INSTANCE);
    }

    // Validate appending data with pre-existing content in the buffer
    @Test
    @DisplayName("Appends Base85 text with existing data")
    void testAppendWithExistingData() {
        LongConverterTestSupport.assertAppendWithPrefix(TEST_STRING, Base85LongConverter.INSTANCE, PREFIX_HELLO);
    }

    // Ensure safe character conversion using TextWire
    @Test
    @DisplayName("Allows safe Base85 characters in TextWire")
    void allSafeCharsTextWire() {
        // Create a TextWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        assertInstanceOf(TextWire.class, wire, "wire instance should be TextWire for base85 safe character validation");
        // Execute the generic safe character check
        LongConverterTestSupport.allSafeChars(wire, Base85LongConverter.INSTANCE);
    }

    // Ensure safe character conversion using YamlWire
    @Test
    @DisplayName("Allows safe Base85 characters in YamlWire")
    void allSafeCharsYamlWire() {
        // Create a YamlWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new YamlWire();
        assertInstanceOf(YamlWire.class, wire, "wire instance should be YamlWire for base85 safe character validation");
        // Execute the generic safe character check
        LongConverterTestSupport.allSafeChars(wire, Base85LongConverter.INSTANCE);
    }
}
