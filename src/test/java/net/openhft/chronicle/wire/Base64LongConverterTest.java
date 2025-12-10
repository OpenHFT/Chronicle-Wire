/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertTrue(true);
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
        LongConverterTestSupport.allSafeChars(wire, Base64LongConverter.INSTANCE, 64 * 64L);
    }

    // Ensure safe character conversion using YamlWire
    @Test
    public void allSafeCharsYamlWire() {
        // Create a YamlWire instance with elastic on heap bytes and configure it to use text documents
        Wire wire = new YamlWire();
        // Execute the generic safe character check
        LongConverterTestSupport.allSafeChars(wire, Base64LongConverter.INSTANCE, 64 * 64L);
    }
}
