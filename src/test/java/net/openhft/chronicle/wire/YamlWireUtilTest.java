package net.openhft.chronicle.wire;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static net.openhft.chronicle.wire.YamlWire.readNumberOrTextFrom;
import static net.openhft.chronicle.wire.YamlWire.removeUnderscore;
import static org.junit.Assert.*;

public class YamlWireUtilTest {
    @Test
    public void testRemoveUnderscoreFromString() {
        StringBuilder s = new StringBuilder("_a_b_c_d_");
        removeUnderscore(s);
        assertEquals("abcd", s.toString());
    }

    @Test
    public void removeUnderscoreNoChange() {
        // Test case where no underscores are present.
        StringBuilder s = new StringBuilder("abcd");
        removeUnderscore(s);
        assertEquals("abcd", s.toString());
    }

    @Test
    public void testReadNumberOrTextFromNull() {
        assertNull(readNumberOrTextFrom('\0', null));
    }

    @Test
    public void testReadNumberOrTextFromNonZeroBeginQuote() {
        StringBuilder s = new StringBuilder("Hello");
        assertSame(s, readNumberOrTextFrom('"', s));
    }

    @Test
    public void readNumberOrTextFromInvalidFirstChar() {
        // Test case where first character is not in the list "0123456789.+-"
        StringBuilder s = new StringBuilder("Hello");
        assertSame(s, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromEmptyString() {
        StringBuilder s2 = new StringBuilder();
        assertSame(s2, readNumberOrTextFrom('\0', s2));
    }

    @Test
    public void readNumberOrTextFromMaxLength() {
        // Test case where length is exactly 40.
        StringBuilder s = new StringBuilder("a012345678901234567890123456789012345678");
        assertSame(s, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromExceedMaxLengthString() {
        StringBuilder s3 = new StringBuilder("a0123456789012345678901234567890123456789");
        assertSame(s3, readNumberOrTextFrom('\0', s3));
    }

    @Test
    public void testReadNumberOrTextFromSingleCharacter() {
        StringBuilder sch = new StringBuilder("a");
        assertEquals(sch, readNumberOrTextFrom('\0', sch));
    }

    @Test
    public void testReadNumberOrTextFromNumberWithUnderscores() {
        StringBuilder s = new StringBuilder("1_2_1_");
        assertEquals((long) 121, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextLooksLikeANumberWithUnderscores() {
        String str = "1_123_A";
        StringBuilder s = new StringBuilder(str);
        assertSame(s, readNumberOrTextFrom('\0', s));
        assertEquals(str, s.toString());
    }

    @Test
    public void testReadNumberOrTextFromOctalNumber() {
        StringBuilder s = new StringBuilder("0o70");
        assertEquals((long) 070, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void readNumberOrTextFromInvalidNumber() {
        // Test case where the number is not in correct format.
        StringBuilder s = new StringBuilder("0o8");
        // doesn't check the octal is actually octal.
        assertEquals(8.0, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromNegativeNumber() {
        StringBuilder s = new StringBuilder("-127");
        assertEquals((long) -127, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromNegativeDecimalNumber() {
        StringBuilder s = new StringBuilder("-127.0");
        assertEquals((double) -127, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromShortFormTime() {
        StringBuilder s = new StringBuilder("1:23:45");
        assertEquals(LocalTime.parse("0" + s), readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromFullFormTime() {
        StringBuilder s = new StringBuilder("11:23:45");
        assertEquals(LocalTime.parse(s), readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromInvalidDate() {
        StringBuilder s = new StringBuilder("2023-07-32");
        assertSame(s, readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromDate() {
        StringBuilder s = new StringBuilder("2023-07-27");
        assertEquals(LocalDate.parse(s), readNumberOrTextFrom('\0', s));
    }

    @Test
    public void testReadNumberOrTextFromDateTime() {
        StringBuilder s = new StringBuilder("2023-07-27T12:34:56.789Z");
        assertEquals(ZonedDateTime.parse(s), readNumberOrTextFrom('\0', s));
    }

}
