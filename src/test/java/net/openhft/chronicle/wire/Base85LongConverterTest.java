/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
        // Create an elastic byte buffer
        final Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            // Obtain the singleton instance of Base85LongConverter
            final Base85LongConverter idLongConverter = Base85LongConverter.INSTANCE;
            // Parse the TEST_STRING into a long value
            final long helloWorld = idLongConverter.parse(TEST_STRING);
            // Append the parsed value back into a byte buffer
            idLongConverter.append(b, helloWorld);
            // Validate the byte buffer content against the TEST_STRING
            assertEquals(TEST_STRING, b.toString());
        } finally {
            // Release the allocated buffer
            b.releaseLast();
        }
    }

    // Validate appending data with pre-existing content in the buffer
    @Test
    public void testAppendWithExistingData() {
        // Create an elastic byte buffer and append "hello" to it
        final Bytes<?> b = Bytes.elasticByteBuffer().append("hello");
        try {
            // Obtain the singleton instance of Base85LongConverter
            final Base85LongConverter idLongConverter = Base85LongConverter.INSTANCE;
            // Parse the TEST_STRING into a long value
            final long helloWorld = idLongConverter.parse(TEST_STRING);
            // Append the parsed value to the already partially filled byte buffer
            idLongConverter.append(b, helloWorld);
            // Validate the byte buffer content against the concatenated string "hello" + TEST_STRING
            assertEquals("hello" + TEST_STRING, b.toString());
        } finally {
            // Release the allocated buffer
            b.releaseLast();
        }
    }

    // Ensure safe character conversion using TextWire
    @Test
    public void allSafeCharsTextWire() {
        // Create a TextWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        // Execute the generic safe character check
        allSafeChars(wire);
    }

    // Ensure safe character conversion using YamlWire
    @Test
    public void allSafeCharsYamlWire() {
        // Create a YamlWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        // Execute the generic safe character check
        allSafeChars(wire);
    }

    // Validate all safe characters within the provided Wire instance
    private void allSafeChars(Wire wire) {
        // Obtain the singleton instance of Base85LongConverter
        final Base85LongConverter converter = Base85LongConverter.INSTANCE;
        // Iterate through long numbers, validating their conversion and sequencing in the Wire
        for (long i = 0; i <= 85 * 85; i++) {
            // Clear the wire data
            wire.clear();
            // Write the long number i into the wire with a tag "a" using the converter
            wire.write("a").writeLong(converter, i);
            // Write a sequence of the same number tagged as "b"
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });
            // Validate that the wire representation is accurate
            assertEquals(wire.toString(),
                    i, wire.read("a").readLong(converter));
            // Check the sequence integrity and correctness in the wire
            wire.read("b").sequence(i, (i2, v) -> {
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
