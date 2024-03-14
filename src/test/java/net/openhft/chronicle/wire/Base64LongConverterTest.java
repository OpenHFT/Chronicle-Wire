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
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class Base64LongConverterTest extends WireTestCommon {

    // Validate the parsing of Base64 encoded strings to long and vice versa
    @Test
    public void parse() {
        // Obtain the singleton instance of Base64LongConverter
        LongConverter c = Base64LongConverter.INSTANCE;
        // System.out.println(c.asString(-1L));
        // Iterate through predefined strings, validate conversion from string to long and back to string
        for (String s : ",a,ab,abc,abcd,ab.de,123_56,1234567,12345678,123456789,z23456789,z234567890,O_________".split(",")) {
            long v = c.parse(s);
            assertEquals(s, c.asString(v));
        }
    }

    // Validate string conversion of randomly generated long numbers
    @Test
    public void parseSubsequence() {
        LongConverter c = Base64LongConverter.INSTANCE;
        String s = ",a,ab,abc,abcd,ab.de,123_56,1234567,12345678,123456789,z23456789,z234567890,O_________,";
        int comparisons = 13;
        subStringParseLoop(s, c, comparisons);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseLengthCheck() {
        Base64LongConverter.INSTANCE.parse(getClass().getCanonicalName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseSubstringLengthCheck() {
        Base64LongConverter.INSTANCE.parse("abcd", 0, 5);
    }

    @Test
    public void parseSubsequence() {
        LongConverter c = Base64LongConverter.INSTANCE;
        String s = ",a,ab,abc,abcd,ab.de,123_56,1234567,12345678,123456789,z23456789,z234567890,O_________,";
        int comparisons = 13;
        subStringParseLoop(s, c, comparisons);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseLengthCheck() {
        Base64LongConverter.INSTANCE.parse(getClass().getCanonicalName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseSubstringLengthCheck() {
        Base64LongConverter.INSTANCE.parse("abcd", 0, 5);
    }

    @Test
    public void asString() {
        // Obtain the singleton instance of Base64LongConverter
        LongConverter c = Base64LongConverter.INSTANCE;
        // Initialize a random number generator
        Random rand = new Random();

        // Validate the conversion of 128 randomly generated long numbers
        for (int i = 0; i < 128; i++) {
            // Ensure random consistency by seeding with the loop variable
            rand.setSeed(i);
            long l = rand.nextLong();
            // Convert the long number to a Base64 encoded string
            String s = c.asString(l);
            // Assert conversion consistency by parsing it back and comparing with the original long number
            Assert.assertEquals("i: " + i + ", s: " + s, l, c.parse(s));
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

    // Utility method: Validate all safe characters within the provided Wire instance
    private void allSafeChars(Wire wire) {
        // Obtain the singleton instance of Base64LongConverter
        final LongConverter converter = Base64LongConverter.INSTANCE;
        // Iterate through long numbers, validating their conversion and sequencing in the Wire
        for (long i = 0; i <= 64 * 64; i++) {
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
