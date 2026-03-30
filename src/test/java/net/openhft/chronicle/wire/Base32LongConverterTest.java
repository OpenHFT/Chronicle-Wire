/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base32LongConverterTest extends WireTestCommon {

    // A test to check the parsing functionality of Base32LongConverter.
    @Test
    void parse() {
        LongConverter bic = new Base32LongConverter();

        // Iterate through predefined string values to check the conversion consistency
        // in both original and lower case forms.
        for (String s : ",O,A,L,ZZ,QQ,ABCDEGHIJKLM,5OPQRSTVWXYZ,JZZZZZZZZZZZ".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)));  // Check if parsing and then converting back to string remains consistent with the original string
            assertEquals(s, bic.asString(bic.parse(s.toLowerCase()))); // Do the same for the lower case version
        }
    }

    // A test to check the character safety in TextWire.
    @Test
    void parseSubsequence() {
        LongConverter c = Base32LongConverter.INSTANCE;
        String s = ",O,A,L,ZZ,QQ,ABCDEGHIJKLM,5OPQRSTVWXYZ,JZZZZZZZZZZZ,";
        int comparisons = 9;
        subStringParseLoop(s, c, comparisons);
        assertTrue(true);
    }

    @Test
    void parseLengthCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            Base32LongConverter.INSTANCE.parse(getClass().getCanonicalName());
        });
    }

    @Test
    void parseSubstringLengthCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            Base32LongConverter.INSTANCE.parse("ABCD", 3, 0);
        });
    }

    @Test
    void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    // A test to check the character safety in YamlWire.
    @Test
    void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        allSafeChars(wire);
    }

    // A method that performs a check on all safe characters in a given wire format.
    private void allSafeChars(Wire wire) {
        // Retrieve an instance of Base32LongConverter
        final LongConverter converter = Base32LongConverter.INSTANCE;

        // Iterating over a set of long numbers, to validate the consistency
        // of writing a long to the wire and reading it back.
        for (long i = 0; i <= 32 * 32; i++) {
            wire.clear();  // Clear the wire content
            wire.write("a").writeLong(converter, i); // Write a long value using the converter
            wire.write("b").sequence(i, (i2, v) -> {
                // Write a sequence of long values using the converter
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });
            // Validate that the read value matches the written value.
            assertEquals(i, wire.read("a").readLong(converter), wire.toString());
            wire.read("b").sequence(i, (i2, v) -> {
                // Validate that the sequence read values match the written values.
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
