/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.converter.SizeLongConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SizeLongConverterTest {

    private final SizeLongConverter converter = SizeLongConverter.INSTANCE;

    @Test
    public void testParse() {
        // Test parsing without suffix
        Assertions.assertEquals(123, converter.parse("123"));

        // Test parsing with each suffix
        Assertions.assertEquals(0, converter.parse("0k"));
        Assertions.assertEquals(2 * 1024, converter.parse("2k"));
        Assertions.assertEquals(21 * 1024, converter.parse("21K"));
        Assertions.assertEquals(3 * 1024 * 1024, converter.parse("3m"));
        Assertions.assertEquals(31 * 1024 * 1024, converter.parse("31M"));
        Assertions.assertEquals(5 * 1024L * 1024 * 1024, converter.parse("5g"));
        Assertions.assertEquals(51 * 1024L * 1024 * 1024, converter.parse("51G"));
        Assertions.assertEquals(7 * 1024L * 1024 * 1024 * 1024, converter.parse("7t"));
        Assertions.assertEquals(71 * 1024L * 1024 * 1024 * 1024, converter.parse("71T"));
    }

    @Test
    public void testParseInvalidNumber() {
        assertThrows(NumberFormatException.class, () ->
                converter.parse("invalid"));
    }

    @Test
    public void testParseEmptyString() {
        assertThrows(NumberFormatException.class, () ->
                converter.parse(""));
    }

    @Test
    public void testParseNoDigit() {
        assertThrows(NumberFormatException.class, () ->
                converter.parse("g"));
    }

    @Test
    public void testAppend() {
        Assertions.assertEquals("0", converter.asString(0));

        // Test appending without needing a suffix
        Assertions.assertEquals("123", converter.asString(123));

        // Test appending with each suffix
        Assertions.assertEquals("2K", converter.asString(2 << 10));

        Assertions.assertEquals("3M", converter.asString(3 << 20));

        Assertions.assertEquals("4G", converter.asString(4L << 30));

        Assertions.assertEquals("5T", converter.asString(5L << 40));
    }

    @Test
    public void testAppendNonExactPowersOf1024() {
        // Values that are not exact multiples of 1024^x should not have a suffix
        Assertions.assertEquals("1025", converter.asString(1025)); // Just above 1K
        Assertions.assertEquals("1048577", converter.asString(1 << 20 | 1)); // Just above 1M
        Assertions.assertEquals(Long.toString(1L << 40 | 1), converter.asString(1L << 40 | 1)); // Just above 1T
    }

    @Test
    public void testAppendWithNegativeValues() {
        // Testing negative values
        Assertions.assertEquals("-1K", converter.asString(-1024)); // -1K
        Assertions.assertEquals("-1M", converter.asString(-(1 << 20))); // -1M
    }
}
