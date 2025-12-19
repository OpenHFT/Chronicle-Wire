/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.IdentifierLongConverter.*;
import static org.junit.jupiter.api.Assertions.*;

public class IdentifierLongConverterTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Defining constants for the test class
    private static final String MAX_SMALL_POSITIVE_STR = "^^^^^^^^^^";

    // Test the parsing functionality for the minimum values
    @Test
    public void parseMin() {
        assertEquals(0, INSTANCE.parse(""));
        assertEquals(0, INSTANCE.parse("000", 1, 2));
    }

    // Test parsing functionality for the maximum small positive value
    @Test
    public void parseMaxSmallPositive() {
        assertEquals(MAX_SMALL_ID,
                INSTANCE.parse(MAX_SMALL_POSITIVE_STR));
    }

    // Test parsing functionality for one more than the max small positive value
    @Test
    public void parseMaxSmallPositivePlus1() {
        assertEquals(MAX_SMALL_ID + 1,
                INSTANCE.parse(MIN_DATE));
    }

    // Test the string representation for zero
    @Test
    public void asString0() {
        assertEquals("",
                INSTANCE.asString(0));
    }

    // Test the string representation for the max small value
    @Test
    public void asStringMaxSmall() {
        assertEquals(MAX_SMALL_POSITIVE_STR,
                INSTANCE.asString(MAX_SMALL_ID));
    }

    // Test the string representation for one more than the max small value
    @Test
    public void asStringMaxSmallPlus1() {
        assertEquals(MIN_DATE,
                INSTANCE.asString(MAX_SMALL_ID + 1));
    }

    // Test the string representation for the maximum DateTime value
    @Test
    public void asStringMaxDateTime() {
        assertEquals(MAX_DATE,
                INSTANCE.asString(Long.MAX_VALUE));
    }

    // Test using the TextWire format with safe characters
    @Test
    public void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        assertInstanceOf(TextWire.class, wire, "safe chars: wire type");
        LongConverterTestSupport.allSafeChars(wire, IdentifierLongConverter.INSTANCE, 31);
    }

    // Test using the YamlWire format with safe characters
    @Test
    public void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        assertInstanceOf(YamlWire.class, wire, "safe chars: wire type");
        LongConverterTestSupport.allSafeChars(wire, IdentifierLongConverter.INSTANCE, 31);
    }
}
