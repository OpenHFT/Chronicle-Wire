package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.converter.SizeLongConverter;
import org.junit.Assert;
import org.junit.Test;

public class SizeLongConverterTest {

    private SizeLongConverter converter = SizeLongConverter.INSTANCE;

    @Test
    public void testParse() {
        // Test parsing without suffix
        Assert.assertEquals(123, converter.parse("123"));

        // Test parsing with each suffix
        Assert.assertEquals(0, converter.parse("0k"));
        Assert.assertEquals(2 * 1024, converter.parse("2k"));
        Assert.assertEquals(21 * 1024, converter.parse("21K"));
        Assert.assertEquals(3 * 1024 * 1024, converter.parse("3m"));
        Assert.assertEquals(31 * 1024 * 1024, converter.parse("31M"));
        Assert.assertEquals(5 * 1024L * 1024 * 1024, converter.parse("5g"));
        Assert.assertEquals(51 * 1024L * 1024 * 1024, converter.parse("51G"));
        Assert.assertEquals(7 * 1024L * 1024 * 1024 * 1024, converter.parse("7t"));
        Assert.assertEquals(71 * 1024L * 1024 * 1024 * 1024, converter.parse("71T"));
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidNumber() {
        converter.parse("invalid");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseEmptyString() {
        converter.parse("");
    }

    @Test
    public void testAppend() {
        Assert.assertEquals("0", converter.asString(0));

        // Test appending without needing a suffix
        Assert.assertEquals("123", converter.asString(123));

        // Test appending with each suffix
        Assert.assertEquals("2K", converter.asString(2 << 10));

        Assert.assertEquals("3M", converter.asString(3 << 20));

        Assert.assertEquals("4G", converter.asString(4L << 30));

        Assert.assertEquals("5T", converter.asString(5L << 40));
    }

    @Test
    public void testAppendNonExactPowersOf1024() {
        // Values that are not exact multiples of 1024^x should not have a suffix
        Assert.assertEquals("1025", converter.asString(1025)); // Just above 1K
        Assert.assertEquals("1048577", converter.asString(1 << 20 | 1)); // Just above 1M
        Assert.assertEquals(Long.toString(1L << 40 | 1), converter.asString(1L << 40 | 1)); // Just above 1T
    }

    @Test
    public void testAppendWithNegativeValues() {
        // Testing negative values
        Assert.assertEquals("-1K", converter.asString(-1024)); // -1K
        Assert.assertEquals("-1M", converter.asString(-(1 << 20))); // -1M
    }
}
