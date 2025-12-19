/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(true);
    }

    @Test
    public void parseLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                Base32LongConverter.INSTANCE.parse(getClass().getCanonicalName()));
    }

    @Test
    public void parseSubstringLengthCheck() {
        assertThrows(IllegalArgumentException.class, () ->
                Base32LongConverter.INSTANCE.parse("ABCD", 3, 0));
    }

    @Test
    public void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        assertInstanceOf(TextWire.class, wire, "safe chars: wire type");
        LongConverterTestSupport.allSafeChars(wire, Base32LongConverter.INSTANCE, 32 * 32L);
    }

    // A test to check the character safety in YamlWire.
    @Test
    public void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        assertInstanceOf(YamlWire.class, wire, "safe chars: wire type");
        LongConverterTestSupport.allSafeChars(wire, Base32LongConverter.INSTANCE, 32 * 32L);
    }
}
