package net.openhft.chronicle.wire;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static net.openhft.chronicle.wire.YamlWire.readNumberOrTextFrom;
import static net.openhft.chronicle.wire.YamlWire.removeUnderscore;
import static org.junit.Assert.*;

public class YamlWireUtilTest {

    // Test to validate the removal of underscores from a string with leading/trailing/interstitial underscores
    @Test
    public void testRemoveUnderscoreFromString() {
        StringBuilder s = new StringBuilder("_a_b_c_d_");
        removeUnderscore(s);
        assertEquals("abcd", s.toString());
    }

    // Test to confirm the behavior of the removeUnderscore method when no underscores are present in the string
    @Test
    public void removeUnderscoreNoChange() {
        // Test case where no underscores are present.
        StringBuilder s = new StringBuilder("abcd");
        removeUnderscore(s);
        assertEquals("abcd", s.toString());
    }

    // Test to ensure the readNumberOrTextFrom method returns null when provided a null input and a '\0' character
    @Test
    public void testReadNumberOrTextFromNull() {
        assertNull(readNumberOrTextFrom('\0', null));
    }

    // Test to ensure that readNumberOrTextFrom method returns the original input when it begins with a non-zero quote character
    @Test
    public void testReadNumberOrTextFromNonZeroBeginQuote() {
        StringBuilder s = new StringBuilder("Hello");
        assertSame(s, readNumberOrTextFrom('"', s));
    }

    // Test to validate the behavior of readNumberOrTextFrom method when the first character isn't a valid starting numeric character
    @Test
    public void readNumberOrTextFromInvalidFirstChar() {
        // Test case where first character is not in the list "0123456789.+-"
        StringBuilder s = new StringBuilder("Hello");
        assertSame(s, readNumberOrTextFrom('\0', s));
    }

    // Test to confirm the readNumberOrTextFrom method returns the same object when given an empty input string
    @Test
    public void testReadNumberOrTextFromEmptyString() {
        StringBuilder s2 = new StringBuilder();
        assertSame(s2, readNumberOrTextFrom('\0', s2));
    }

    // Test case that checks if the readNumberOrTextFrom method behaves correctly with a max valid length string (40 characters)
    @Test
    public void readNumberOrTextFromMaxLength() {
        // Test case where length is exactly 40.
        StringBuilder s = new StringBuilder("a012345678901234567890123456789012345678");
        assertSame(s, readNumberOrTextFrom('\0', s));
    }

    // Test to ensure the readNumberOrTextFrom method behaves correctly when a string of length > 40 characters is passed to it
    @Test
    public void testReadNumberOrTextFromExceedMaxLengthString() {
        StringBuilder s3 = new StringBuilder("a0123456789012345678901234567890123456789");
        assertSame(s3, readNumberOrTextFrom('\0', s3));
    }

    // Test to verify that readNumberOrTextFrom method returns the same object when a single-character string is passed to it
    @Test
    public void testReadNumberOrTextFromSingleCharacter() {
        StringBuilder sch = new StringBuilder("a");
        assertEquals(sch, readNumberOrTextFrom('\0', sch));
    }

    // Test to validate the correct parsing of a number with underscores by readNumberOrTextFrom method
    @Test
    public void testReadNumberOrTextFromNumberWithUnderscores() {
        StringBuilder s = new StringBuilder("1_2_1_");
        assertEquals((long) 121, readNumberOrTextFrom('\0', s));
    }

    // Test to confirm the behavior of the readNumberOrTextFrom method when it encounters a string that partially resembles a number but includes non-numeric characters
    @Test
    public void testReadNumberOrTextLooksLikeANumberWithUnderscores() {
        String str = "1_123_A";
        StringBuilder s = new StringBuilder(str);
        assertSame(s, readNumberOrTextFrom('\0', s));
        assertEquals(str, s.toString());
    }

    // Test to ensure the readNumberOrTextFrom method correctly handles octal numbers prefixed with '0o'
    @Test
    public void testReadNumberOrTextFromOctalNumber() {
        StringBuilder s = new StringBuilder("0o70");
        assertEquals((long) 070, readNumberOrTextFrom('\0', s));
    }

    // Test to validate that the readNumberOrTextFrom method can handle an invalid octal number by not checking the validity of the octal format
    @Test
    public void readNumberOrTextFromInvalidNumber() {
        // Test case where the number is not in correct format.
        StringBuilder s = new StringBuilder("0o8");
        // doesn't check the octal is actually octal.
        assertEquals(8.0, readNumberOrTextFrom('\0', s));
    }

    // Test to confirm the behavior of readNumberOrTextFrom method when given a negative whole number
    @Test
    public void testReadNumberOrTextFromNegativeNumber() {
        StringBuilder s = new StringBuilder("-127");
        assertEquals((long) -127, readNumberOrTextFrom('\0', s));
    }

    // Test to validate the behavior of the readNumberOrTextFrom method when it encounters a negative decimal number
    @Test
    public void testReadNumberOrTextFromNegativeDecimalNumber() {
        StringBuilder s = new StringBuilder("-127.0");
        assertEquals((double) -127, readNumberOrTextFrom('\0', s));
    }

    // Test to confirm the readNumberOrTextFrom method can correctly interpret a string representing a short-form time (without leading zero on the hour)
    @Test
    public void testReadNumberOrTextFromShortFormTime() {
        StringBuilder s = new StringBuilder("1:23:45");
        assertEquals(LocalTime.parse("0" + s), readNumberOrTextFrom('\0', s));
    }

    // Test to validate the behavior of the readNumberOrTextFrom method when given a string representing time in HH:mm:ss format
    @Test
    public void testReadNumberOrTextFromFullFormTime() {
        StringBuilder s = new StringBuilder("11:23:45");
        assertEquals(LocalTime.parse(s), readNumberOrTextFrom('\0', s));
    }

    // Test to ensure the readNumberOrTextFrom method behaves correctly when provided a string representing an invalid date
    @Test
    public void testReadNumberOrTextFromInvalidDate() {
        StringBuilder s = new StringBuilder("2023-07-32");
        assertSame(s, readNumberOrTextFrom('\0', s));
    }

    // Test to confirm the behavior of readNumberOrTextFrom method when given a valid date string
    @Test
    public void testReadNumberOrTextFromDate() {
        StringBuilder s = new StringBuilder("2023-07-27");
        assertEquals(LocalDate.parse(s), readNumberOrTextFrom('\0', s));
    }

    // Test to validate that the readNumberOrTextFrom method can correctly parse a string representing a date-time in ISO-8601 format
    @Test
    public void testReadNumberOrTextFromDateTime() {
        StringBuilder s = new StringBuilder("2023-07-27T12:34:56.789Z");
        assertEquals(ZonedDateTime.parse(s), readNumberOrTextFrom('\0', s));
    }

}
