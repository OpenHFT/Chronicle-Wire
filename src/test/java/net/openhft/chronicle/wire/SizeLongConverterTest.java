/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.converter.SizeLongConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SizeLongConverterTest {

    private final SizeLongConverter converter = SizeLongConverter.INSTANCE;

    @Test
    @DisplayName("Size converter parses values with suffixes")
    public void testParse() {
        // Test parsing without suffix
        Assertions.assertEquals(123, converter.parse("123"), "Parse should return 123 without suffix");

        // Test parsing with each suffix
        Assertions.assertEquals(0, converter.parse("0k"), "Parse should return 0 for 0k");
        Assertions.assertEquals(2 * 1024, converter.parse("2k"), "Parse should return 2048 for 2k");
        Assertions.assertEquals(21 * 1024, converter.parse("21K"), "Parse should return 21504 for 21K");
        Assertions.assertEquals(3 * 1024 * 1024, converter.parse("3m"), "Parse should return 3M in bytes");
        Assertions.assertEquals(31 * 1024 * 1024, converter.parse("31M"), "Parse should return 31M in bytes");
        Assertions.assertEquals(5 * 1024L * 1024 * 1024, converter.parse("5g"), "Parse should return 5G in bytes");
        Assertions.assertEquals(51 * 1024L * 1024 * 1024, converter.parse("51G"), "Parse should return 51G in bytes");
        Assertions.assertEquals(7 * 1024L * 1024 * 1024 * 1024, converter.parse("7t"), "Parse should return 7T in bytes");
        Assertions.assertEquals(71 * 1024L * 1024 * 1024 * 1024, converter.parse("71T"), "Parse should return 71T in bytes");
    }

    @Test
    @DisplayName("Size converter rejects invalid numeric input")
    public void testParseInvalidNumber() {
        assertThrows(NumberFormatException.class, () ->
                converter.parse("invalid"), "Invalid number input should raise NumberFormatException");
    }

    @Test
    @DisplayName("Size converter rejects empty input string")
    public void testParseEmptyString() {
        assertThrows(NumberFormatException.class, () ->
                converter.parse(""), "Empty input should raise NumberFormatException error");
    }

    @Test
    @DisplayName("Size converter rejects input without digits")
    public void testParseNoDigit() {
        assertThrows(NumberFormatException.class, () ->
                converter.parse("g"), "Input without digits should raise NumberFormatException");
    }

    @Test
    @DisplayName("Size converter formats values with suffixes")
    public void testAppend() {
        Assertions.assertEquals("0", converter.asString(0), "Format should return 0 for zero");

        // Test appending without needing a suffix
        Assertions.assertEquals("123", converter.asString(123), "Format should keep plain decimal value");

        // Test appending with each suffix
        Assertions.assertEquals("2K", converter.asString(2 << 10), "Format should apply K suffix here");

        Assertions.assertEquals("3M", converter.asString(3 << 20), "Format should apply M suffix here");

        Assertions.assertEquals("4G", converter.asString(4L << 30), "Format should apply G suffix here");

        Assertions.assertEquals("5T", converter.asString(5L << 40), "Format should apply T suffix here");
    }

    @Test
    @DisplayName("Size converter keeps non power values")
    public void testAppendNonExactPowersOf1024() {
        // Values that are not exact multiples of 1024^x should not have a suffix
        Assertions.assertEquals("1025", converter.asString(1025),
                "Non exact 1K should keep full value");
        Assertions.assertEquals("1048577", converter.asString(1 << 20 | 1),
                "Non exact 1M should keep full value");
        Assertions.assertEquals(Long.toString(1L << 40 | 1), converter.asString(1L << 40 | 1),
                "Non exact 1T should keep full value");
    }

    @Test
    @DisplayName("Size converter formats negative power values")
    public void testAppendWithNegativeValues() {
        // Testing negative values
        Assertions.assertEquals("-1K", converter.asString(-1024),
                "Negative 1K should format with K suffix");
        Assertions.assertEquals("-1M", converter.asString(-(1 << 20)),
                "Negative 1M should format with M suffix");
    }
}
