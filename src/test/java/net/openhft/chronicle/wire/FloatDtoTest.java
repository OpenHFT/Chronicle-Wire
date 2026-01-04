/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FloatDtoTest extends WireTestCommon {

    // Test method to serialize and deserialize a 'Value' object using a wire
    @Test
    @DisplayName("Serialises and reads float dto values")
    public void test() {
        // Creating a 'Value' instance with specific values
        @NotNull final Value value = new Value(99, 2000f);
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        final Wire w = WireType.BINARY.apply(bytes);

        // Serializing the 'value' object to the wire
        w.write().marshallable(value);

        // Initializing another 'Value' instance with default values
        @NotNull Value object1 = new Value(0, 0.0f);

        // Deserializing data from the wire into the 'object1' instance
        w.read().marshallable(object1);

        // Asserting that the original 'value' and the deserialized 'object1' are equal
        assertEquals(value, object1,
                "marshalled and unmarshalled value should match");
        assertEquals(99, object1.uiid,
                "uiid should round-trip for float dto");
        assertEquals(2000f, object1.myFloat, 0.0f,
                "myFloat should round-trip for float dto");
        bytes.releaseLast();
    }

    // Inner static class 'Key' with a unique ID attribute
    static class Key extends SelfDescribingMarshallable implements KeyedMarshallable {
        // Suppress unused warning as the field may be used for serialization/deserialization purposes
        @SuppressWarnings("unused")
        final
        int uiid;

        // Constructor to initialize the 'Key' with a unique ID
        Key(int uiid) {
            this.uiid = uiid;
        }
    }

    // Inner static class 'Value' that extends 'Key' and has an additional float attribute
    static class Value extends Key implements Marshallable {
        // Suppress unused warning as the field may be used for serialization/deserialization purposes
        @SuppressWarnings("unused")
        final float myFloat;

        // Constructor to initialize the 'Value' with a unique ID and a float
        Value(int uiid,
              float myFloat) {
            super(uiid);
            this.myFloat = myFloat;
        }
    }
}
