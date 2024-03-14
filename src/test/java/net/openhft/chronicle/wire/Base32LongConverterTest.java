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

import static org.junit.Assert.assertEquals;

public class Base32LongConverterTest extends WireTestCommon {

    // A test to check the parsing functionality of Base32LongConverter.
    @Test
    public void parse() {
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
    public void parseSubsequence() {
        LongConverter c = Base32LongConverter.INSTANCE;
        String s = ",O,A,L,ZZ,QQ,ABCDEGHIJKLM,5OPQRSTVWXYZ,JZZZZZZZZZZZ,";
        int comparisons = 9;
        subStringParseLoop(s, c, comparisons);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseLengthCheck() {
        Base32LongConverter.INSTANCE.parse(getClass().getCanonicalName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseSubstringLengthCheck() {
        Base32LongConverter.INSTANCE.parse("ABCD", 3, 0);
    }

    @Test
    public void parseSubsequence() {
        LongConverter c = Base32LongConverter.INSTANCE;
        String s = ",O,A,L,ZZ,QQ,ABCDEGHIJKLM,5OPQRSTVWXYZ,JZZZZZZZZZZZ,";
        int comparisons = 9;
        subStringParseLoop(s, c, comparisons);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseLengthCheck() {
        Base32LongConverter.INSTANCE.parse(getClass().getCanonicalName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseSubstringLengthCheck() {
        Base32LongConverter.INSTANCE.parse("ABCD", 3, 0);
    }

    @Test
    public void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    // A test to check the character safety in YamlWire.
    @Test
    public void allSafeCharsYamlWire() {
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
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
            assertEquals(wire.toString(),
                    i, wire.read("a").readLong(converter));
            wire.read("b").sequence(i, (i2, v) -> {
                // Validate that the sequence read values match the written values.
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
