/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class Base85LongConverterTest extends WireTestCommon {

    // A sample string to test the parsing functionality.
    private static final CharSequence TEST_STRING = "world";

    @Test
    public void parseLeadingZero() {
        LongConverter c = Base85LongConverter.INSTANCE;
        assertEquals(0L, c.parse("0"));
        assertEquals(0L, c.parse("00"));
        assertEquals(0L, c.parse("000"));
        assertEquals(0L, c.parse("0000"));
        assertEquals(0L, c.parse("00000"));
        assertEquals(0L, c.parse("000000"));
        assertEquals(0L, c.parse("0000000"));
        assertEquals(0L, c.parse("00000000"));
        assertEquals(0L, c.parse("000000000"));
        assertEquals(0L, c.parse("0000000000"));
        assertThrows(IllegalArgumentException.class, () -> c.parse("00000000000"));
        assertEquals("", c.asString(0L));
    }

    @Test
    public void parse() {
        // Obtain the singleton instance of Base85LongConverter
        LongConverter c = Base85LongConverter.INSTANCE;
        // System.out.println(c.asString(-1L));
        // Iterate through predefined strings, validate parsing and string reconstruction
        for (String s : ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0".split(",")) {
            // Parse the string into a long value
            long v = c.parse(s);
            // Convert the parsed long value back into a string and validate against the original string
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }

    @Test
    public void parseSubsequence() {
        LongConverter c = Base85LongConverter.INSTANCE;
        String s = ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0,";
        int comparisons = 11;
        subStringParseLoop(s, c, comparisons);
        assertTrue(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseLengthCheck() {
        Base85LongConverter.INSTANCE.parse(getClass().getCanonicalName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseSubstringLengthCheck() {
        Base85LongConverter.INSTANCE.parse("ABCD", -1, 3);
    }

    @Test
    public void asString() {
        // Obtain the singleton instance of Base85LongConverter
        LongConverter c = Base85LongConverter.INSTANCE;
        IntStream.range(0, 10_000_000)
                .parallel()
                .mapToLong(i -> ThreadLocalRandom.current().nextLong())
                .forEach(l -> {
                    String s = c.asString(l);
                    assertEquals(s, l, c.parse(s));
                });
    }

    // Validate the append operation for a known input string
    @Test
    public void testAppend() {
        LongConverterTestSupport.assertAppend(TEST_STRING, Base85LongConverter.INSTANCE);
    }

    // Validate appending data with pre-existing content in the buffer
    @Test
    public void testAppendWithExistingData() {
        LongConverterTestSupport.assertAppendWithPrefix(TEST_STRING, Base85LongConverter.INSTANCE, "hello");
    }

    // Ensure safe character conversion using TextWire
    @Test
    public void allSafeCharsTextWire() {
        // Create a TextWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        // Execute the generic safe character check
        LongConverterTestSupport.allSafeChars(wire, Base85LongConverter.INSTANCE);
    }

    // Ensure safe character conversion using YamlWire
    @Test
    public void allSafeCharsYamlWire() {
        // Create a YamlWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new YamlWire();
        // Execute the generic safe character check
        LongConverterTestSupport.allSafeChars(wire, Base85LongConverter.INSTANCE);
    }
}
