/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// This test class is designed to validate the behavior of TextWire when dealing with arrays of types.
public class TextWireTypeArrayTest extends WireTestCommon {

    // This test verifies the unmarshalling behavior for arrays of types using TextWire.
    @Test
    public void shouldUnmarshalArrayOfType() {
        // Initialize the byte storage
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Create a TextWire instance with the provided bytes
        final Wire wire = WireType.TEXT.apply(bytes);
        final HasClasses hasClasses = new HasClasses();

        // Marshall the object into the wire
        wire.getValueOut().typedMarshallable(hasClasses);

        // Define the expected string representation
        final String expected = "" +
                "!net.openhft.chronicle.wire.TextWireTypeArrayTest$HasClasses {\n" +
                "  classes: [ !type String, !type int, !type java.lang.Number ]\n" +
                "}\n";
        assertEquals(expected, bytes.toString());

        // Parse the bytes back to an object and verify its string representation
        final TextWire textWire = TextWire.from(bytes.toString());
        final Object a = textWire.getValueIn().typedMarshallable();
        assertEquals(expected, a.toString());
        bytes.releaseLast();
    }

    // Inner class defining an array of class types
    public static class HasClasses extends SelfDescribingMarshallable {
        public Class<?>[] classes = {String.class, Integer.class, Number.class};
    }
}
