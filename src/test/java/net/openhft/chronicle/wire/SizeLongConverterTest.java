/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.converter.SizeLongConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SizeLongConverterTest {

    private SizeLongConverter converter = SizeLongConverter.INSTANCE;

    @Test
    void testParse() {
        // Test parsing without suffix
        assertEquals(123, converter.parse("123"));

        // Test parsing with each suffix
        assertEquals(0, converter.parse("0k"));
        assertEquals(2 * 1024, converter.parse("2k"));
        assertEquals(21 * 1024, converter.parse("21K"));
        assertEquals(3 * 1024 * 1024, converter.parse("3m"));
        assertEquals(31 * 1024 * 1024, converter.parse("31M"));
        assertEquals(5 * 1024L * 1024 * 1024, converter.parse("5g"));
        assertEquals(51 * 1024L * 1024 * 1024, converter.parse("51G"));
        assertEquals(7 * 1024L * 1024 * 1024 * 1024, converter.parse("7t"));
        assertEquals(71 * 1024L * 1024 * 1024 * 1024, converter.parse("71T"));
    }

    @Test
    void testParseInvalidNumber() {
        assertThrows(NumberFormatException.class, () -> converter.parse("invalid"));
    }

    @Test
    void testParseEmptyString() {
        assertThrows(NumberFormatException.class, () -> converter.parse(""));
    }

    @Test
    void testParseNoDigit() {
        assertThrows(NumberFormatException.class, () -> converter.parse("g"));
    }

    @Test
    void testAppend() {
        assertEquals("0", converter.asString(0));

        // Test appending without needing a suffix
        assertEquals("123", converter.asString(123));

        // Test appending with each suffix
        assertEquals("2K", converter.asString(2 << 10));

        assertEquals("3M", converter.asString(3 << 20));

        assertEquals("4G", converter.asString(4L << 30));

        assertEquals("5T", converter.asString(5L << 40));
    }

    @Test
    void testAppendNonExactPowersOf1024() {
        // Values that are not exact multiples of 1024^x should not have a suffix
        assertEquals("1025", converter.asString(1025)); // Just above 1K
        assertEquals("1048577", converter.asString(1 << 20 | 1)); // Just above 1M
        assertEquals(Long.toString(1L << 40 | 1), converter.asString(1L << 40 | 1)); // Just above 1T
    }

    @Test
    void testAppendWithNegativeValues() {
        // Testing negative values
        assertEquals("-1K", converter.asString(-1024)); // -1K
        assertEquals("-1M", converter.asString(-(1 << 20))); // -1M
    }
}
