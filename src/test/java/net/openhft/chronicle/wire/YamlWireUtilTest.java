/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static net.openhft.chronicle.wire.YamlWire.readNumberOrTextFrom;
import static net.openhft.chronicle.wire.YamlWire.removeUnderscore;
import static org.junit.jupiter.api.Assertions.*;

class YamlWireUtilTest {

    // Test to validate the removal of underscores from a string with leading/trailing/interstitial underscores
    @Test
    @DisplayName("Yaml wire util: remove Underscore From String")
    void testRemoveUnderscoreFromString() {
        StringBuilder s = new StringBuilder("_a_b_c_d_");
        removeUnderscore(s);
        assertEquals("abcd", s.toString(),
                "removeUnderscore should remove leading, trailing, and interstitial underscores");
    }

    // Test to confirm the behavior of the removeUnderscore method when no underscores are present in the string
    @Test
    @DisplayName("Yaml wire util: remove Underscore No Change")
    void removeUnderscoreNoChange() {
        // Test case where no underscores are present.
        StringBuilder s = new StringBuilder("abcd");
        removeUnderscore(s);
        assertEquals("abcd", s.toString(),
                "removeUnderscore should leave strings without underscores unchanged");
    }

    // Test to ensure the readNumberOrTextFrom method returns null when provided a null input and a '\0' character
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Null")
    void testReadNumberOrTextFromNull() {
        assertNull(readNumberOrTextFrom('\0', null),
                "Null input should yield a null parse result");
    }

    // Test to ensure that readNumberOrTextFrom method returns the original input when it begins with a non-zero quote character
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Non Zero Begin Quote")
    void testReadNumberOrTextFromNonZeroBeginQuote() {
        StringBuilder s = new StringBuilder("Hello");
        assertSame(s, readNumberOrTextFrom('"', s),
                "Quoted input should return the original StringBuilder");
    }

    // Test to validate the behavior of readNumberOrTextFrom method when the first character isn't a valid starting numeric character
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Invalid First Char")
    void readNumberOrTextFromInvalidFirstChar() {
        // Test case where first character is not in the list "0123456789.+-"
        StringBuilder s = new StringBuilder("Hello");
        assertSame(s, readNumberOrTextFrom('\0', s),
                "Non-numeric first character should return the original StringBuilder");
    }

    // Test to confirm the readNumberOrTextFrom method returns the same object when given an empty input string
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Empty String")
    void testReadNumberOrTextFromEmptyString() {
        StringBuilder s2 = new StringBuilder();
        assertSame(s2, readNumberOrTextFrom('\0', s2),
                "Empty StringBuilder should be returned unchanged");
    }

    // Test case that checks if the readNumberOrTextFrom method behaves correctly with a max valid length string (40 characters)
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Max Length")
    void readNumberOrTextFromMaxLength() {
        // Test case where length is exactly 40.
        StringBuilder s = new StringBuilder("a012345678901234567890123456789012345678");
        assertSame(s, readNumberOrTextFrom('\0', s),
                "Max length input should be returned unchanged");
    }

    // Test to ensure the readNumberOrTextFrom method behaves correctly when a string of length > 40 characters is passed to it
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Exceed Max Length String")
    void testReadNumberOrTextFromExceedMaxLengthString() {
        StringBuilder s3 = new StringBuilder("a0123456789012345678901234567890123456789");
        assertSame(s3, readNumberOrTextFrom('\0', s3),
                "Overlength input should be returned unchanged");
    }

    // Test to verify that readNumberOrTextFrom method returns the same object when a single-character string is passed to it
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Single Character")
    void testReadNumberOrTextFromSingleCharacter() {
        StringBuilder sch = new StringBuilder("a");
        assertEquals(sch, readNumberOrTextFrom('\0', sch),
                "Single character input should be returned unchanged");
    }

    // Test to validate the correct parsing of a number with underscores by readNumberOrTextFrom method
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Number With Underscores")
    void testReadNumberOrTextFromNumberWithUnderscores() {
        StringBuilder s = new StringBuilder("1_2_1_");
        assertEquals((long) 121, readNumberOrTextFrom('\0', s),
                "Underscored integer should parse to 121");
    }

    // Test to confirm the behavior of the readNumberOrTextFrom method when it encounters a string that partially resembles a number but includes non-numeric characters
    @Test
    @DisplayName("Yaml wire util: read Number Or Text Looks Like ANumber With Underscores")
    void testReadNumberOrTextLooksLikeANumberWithUnderscores() {
        String str = "1_123_A";
        StringBuilder s = new StringBuilder(str);
        assertSame(s, readNumberOrTextFrom('\0', s),
                "Mixed numeric and non-numeric input should return original StringBuilder");
        assertEquals(str, s.toString(),
                "Original content should remain unchanged after parse attempt");
    }

    // Test to ensure the readNumberOrTextFrom method correctly handles octal numbers prefixed with '0o'
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Octal Number")
    void testReadNumberOrTextFromOctalNumber() {
        StringBuilder s = new StringBuilder("0o70");
        assertEquals((long) 070, readNumberOrTextFrom('\0', s),
                "Octal literal should parse to expected value");
    }

    // Test to validate that the readNumberOrTextFrom method can handle an invalid octal number by not checking the validity of the octal format
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Invalid Number")
    void readNumberOrTextFromInvalidNumber() {
        // Test case where the number is not in correct format.
        StringBuilder s = new StringBuilder("0o8");
        // doesn't check the octal is actually octal.
        assertEquals(8.0, readNumberOrTextFrom('\0', s),
                "Invalid octal should fall back to decimal parsing");
    }

    // Test to confirm the behavior of readNumberOrTextFrom method when given a negative whole number
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Negative Number")
    void testReadNumberOrTextFromNegativeNumber() {
        StringBuilder s = new StringBuilder("-127");
        assertEquals((long) -127, readNumberOrTextFrom('\0', s),
                "Negative integer should parse correctly");
    }

    // Test to validate the behavior of the readNumberOrTextFrom method when it encounters a negative decimal number
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Negative Decimal Number")
    void testReadNumberOrTextFromNegativeDecimalNumber() {
        StringBuilder s = new StringBuilder("-127.0");
        assertEquals((double) -127, readNumberOrTextFrom('\0', s),
                "Negative decimal should parse to double");
    }

    // Test to confirm the readNumberOrTextFrom method can correctly interpret a string representing a short-form time (without leading zero on the hour)
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Short Form Time")
    void testReadNumberOrTextFromShortFormTime() {
        StringBuilder s = new StringBuilder("1:23:45");
        assertEquals(LocalTime.parse("0" + s), readNumberOrTextFrom('\0', s),
                "Short-form time should parse with leading zero");
    }

    // Test to validate the behavior of the readNumberOrTextFrom method when given a string representing time in HH:mm:ss format
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Full Form Time")
    void testReadNumberOrTextFromFullFormTime() {
        StringBuilder s = new StringBuilder("11:23:45");
        assertEquals(LocalTime.parse(s), readNumberOrTextFrom('\0', s),
                "Full-form time should parse to LocalTime");
    }

    // Test to ensure the readNumberOrTextFrom method behaves correctly when provided a string representing an invalid date
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Invalid Date")
    void testReadNumberOrTextFromInvalidDate() {
        StringBuilder s = new StringBuilder("2023-07-32");
        assertSame(s, readNumberOrTextFrom('\0', s),
                "Invalid date should return the original StringBuilder");
    }

    // Test to confirm the behavior of readNumberOrTextFrom method when given a valid date string
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Date")
    void testReadNumberOrTextFromDate() {
        StringBuilder s = new StringBuilder("2023-07-27");
        assertEquals(LocalDate.parse(s), readNumberOrTextFrom('\0', s),
                "Valid date should parse to LocalDate");
    }

    // Test to validate that the readNumberOrTextFrom method can correctly parse a string representing a date-time in ISO-8601 format
    @Test
    @DisplayName("Yaml wire util: read Number Or Text From Date Time")
    void testReadNumberOrTextFromDateTime() {
        StringBuilder s = new StringBuilder("2023-07-27T12:34:56.789Z");
        assertEquals(ZonedDateTime.parse(s), readNumberOrTextFrom('\0', s),
                "ISO-8601 date-time should parse to ZonedDateTime");
    }
}
