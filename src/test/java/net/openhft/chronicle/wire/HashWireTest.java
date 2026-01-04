/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashWireTest extends WireTestCommon {

    // Test the hashing capability for a sequence of marshallable entries
    @Test
    @DisplayName("Computes hash for marshallable entry sequence")
    void testHash64() {

        // Calculate a 64-bit hash value for the given wire data structure
        long h = HashWire.hash64(wire ->
                wire.write(() -> "entrySet").sequence(s -> {

                    // First marshallable entry with key and value
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-1")
                            .write(() -> "value").text("value-1"));

                    // Second marshallable entry with key and value
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-2")
                            .write(() -> "value").text("value-2"));
                }));

        // Ensure the computed hash is not 0
        assertNotEquals(0, h, "hash64 should produce non-zero hash value for sequence of marshallable entries");
    }

    // Test the hashing capability for the Field object with a given name
    @Test
    @DisplayName("Computes hash for field with map and list content")
    void testHashWithMap() {
        Field a = new Field("hi");
        a.required.put("k", Required.A);
        a.values.add(EnumValue.A);
        a.used = true;

        Field b = new Field("hi");
        b.required.put("k", Required.A);
        b.values.add(EnumValue.A);
        b.used = true;

        assertEquals(a, b, "field objects with identical content should be equal");
        assertEquals("hi", a.name, "field name should match expected value");
        assertTrue(a.used, "field used flag should be true");
        assertNotEquals(0, a.hashCode(), "field object hashCode should be non-zero for populated field");
        assertEquals(a.hashCode(), b.hashCode(), "equal field objects should produce identical hash codes");
    }

    // Simple enumeration for required fields
    enum Required {
        A
    }

    // Simple enumeration for values
    enum EnumValue {
        A
    }

    // A static inner class representing a Field with properties and behaviors
    static class Field extends SelfDescribingMarshallable implements Cloneable {
        final String name; // The name of the field
        public final Map<String, Required> required = new HashMap<>(); // Map to store required field information
        public final List<EnumValue> values = new ArrayList<>(); // List to store enum values
        public boolean used = false; // Flag to check if the field is used

        // Constructor to initialize a Field with a given name
        Field(String name) {
            this.name = name;
        }

        @Override
        public Field clone() {
            return deepCopy();
        }
    }
}
