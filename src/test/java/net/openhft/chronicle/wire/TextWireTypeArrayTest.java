/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.assertEquals;

// This test class is designed to validate the behavior of TextWire when dealing with arrays of types.
class TextWireTypeArrayTest extends WireTestCommon {

    // This test verifies the unmarshalling behavior for arrays of types using TextWire.
    @Test
    @DisplayName("Unmarshals arrays of types using text wire")
    void shouldUnmarshalArrayOfType() {
        // Initialize the byte storage
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Create a TextWire instance with the provided bytes
        final Wire wire = WireType.TEXT.apply(bytes);
        final HasClasses hasClasses = new HasClasses();

        // Marshall the object into the wire
        wire.getValueOut().typedMarshallable(hasClasses);

        // Define the expected string representation
        final String expected = "!net.openhft.chronicle.wire.TextWireTypeArrayTest$HasClasses {\n" +
                "  classes: [ !type String, !type int, !type java.lang.Number ]\n" +
                "}\n";
        assertEquals(expected, bytes.toString(),
                "text wire output should include class array types");

        // Parse the bytes back to an object and verify its string representation
        final TextWire textWire = TextWire.from(bytes.toString());
        final Object a = textWire.getValueIn().typedMarshallable();
        assertEquals(expected, a.toString(),
                "typed marshallable should round-trip in text wire");
        bytes.releaseLast();
    }

    // Inner class defining an array of class types
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class HasClasses extends SelfDescribingMarshallable {
        public Class<?>[] classes = {String.class, Integer.class, Number.class};
    }
}
