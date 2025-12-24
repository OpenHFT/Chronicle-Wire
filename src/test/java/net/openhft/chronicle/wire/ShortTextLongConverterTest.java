/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class ShortTextLongConverterTest extends WireTestCommon {

    private static final CharSequence TEST_STRING = "world";

    @Test
    public void parseLeadingZero() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        assertEquals(0L, c.parse(""), "empty string should parse to 0L");
        assertEquals(0L, c.parse(" "), "single space should parse to 0L");
        assertEquals(0L, c.parse("  "), "two spaces should parse to 0L");
        assertEquals(84L, c.parse("0"), "single '0' character should parse to encoded long value");
        assertEquals(84L, c.parse(" 0"), "space-prefixed '0' should parse to same value as '0'");
        assertEquals(7224, c.parse("00"), "two '0' characters should parse to encoded long value");
        assertEquals(7224, c.parse("  00"), "space-prefixed '00' should parse to same value as '00'");
        assertEquals(614124, c.parse("000"), "three '0' characters should parse to encoded long value");
        assertEquals(52200624, c.parse("0000"), "four '0' characters should parse to encoded long value");
        assertEquals(4437053124L, c.parse("00000"), "five '0' characters should parse to encoded long value");
        assertEquals(377149515624L, c.parse("000000"), "six '0' characters should parse to encoded long value");
        assertEquals(32057708828124L, c.parse("0000000"), "seven '0' characters should parse to encoded long value");
        assertEquals(2724905250390624L, c.parse("00000000"), "eight '0' characters should parse to encoded long value at max length");
        assertEquals(231616946283203124L, c.parse("000000000"), "nine '0' characters should parse to encoded long value");
        assertEquals(1240696360362714008L, c.parse("0000000000"), "ten '0' characters should parse to encoded long value");
        assertThrows(IllegalArgumentException.class, () -> c.parse("00000000000"), "eleven characters should exceed maximum length and throw exception");
        assertEquals("", c.asString(0L), "zero long value should convert to empty string");
    }

    @Test
    public void parse() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        for (String s : ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0".split(",")) {
            long v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString(), "short text should roundtrip through parse and append: '" + s + "'");
        }
    }

    @Test
    public void parseSubsequence() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        String s = ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0,";
        int comparisons = 11;
        subStringParseLoop(s, c, comparisons);
        assertTrue(true, "substring parsing should complete without exceptions for comma-separated short text values");
    }

    @Test
    public void parseLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                        ShortTextLongConverter.INSTANCE.parse(getClass().getCanonicalName()),
                "parsing text longer than maximum length should throw IllegalArgumentException");
    }

    @Test
    public void parseSubstringLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                        ShortTextLongConverter.INSTANCE.parse("abcd", 3, -2),
                "parsing substring with invalid length should throw IllegalArgumentException");
    }

    @Test
    public void asString() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        long sample = 0x1234_5678_9abc_def0L;
        assertEquals(sample, c.parse(c.asString(sample)), "long value should roundtrip through asString and parse");
        IntStream.range(0, 10_000_000)
                .parallel()
                .mapToLong(i -> ThreadLocalRandom.current().nextLong())
                .forEach(l -> {
                    String s = c.asString(l);
                    assertEquals(l, c.parse(s), "random long value should roundtrip through asString and parse: " + s);
                });
    }

    @Test
    public void testAppend() {
        LongConverterTestSupport.assertAppend(TEST_STRING, ShortTextLongConverter.INSTANCE);
    }

    @Test
    public void testAppendWithExistingData() {
        LongConverterTestSupport.assertAppendWithPrefix(TEST_STRING, ShortTextLongConverter.INSTANCE, "hello");
    }

    @Test
    public void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        assertInstanceOf(TextWire.class, wire, "wire instance should be TextWire for safe character testing");
        LongConverterTestSupport.allSafeChars(wire, ShortTextLongConverter.INSTANCE);
    }

    @Test
    public void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        assertInstanceOf(YamlWire.class, wire, "wire instance should be YamlWire for safe character testing");
        LongConverterTestSupport.allSafeChars(wire, ShortTextLongConverter.INSTANCE);
    }
}
