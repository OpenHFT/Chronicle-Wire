/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryWireHighCodeTest extends WireTestCommon {

    // Test if the BinaryWireHighCode values are unique and no duplicates exist
    @Test
    @DisplayName("Ensures BinaryWireHighCode values are unique across all fields")
    void testUnique() throws IllegalAccessException {
        // Ensure there are no pre-defined values
        assertEquals(0, BinaryWireHighCode.values().length,
                "BinaryWireHighCode should have no enum values");

        // Create a set to store the values
        Set<Integer> values = new HashSet<>();

        // Loop through each field in the BinaryWireHighCode class
        for (Field field : BinaryWireHighCode.class.getFields()) {
            // Retrieve the integer value of the current field
            int value = (Integer) field.get(null);

            // Add the value to the set and ensure it's unique (not already present)
            assertTrue(values.add(value),
                    "BinaryWireHighCode value should be unique for field=" + field.getName() + ", value=" + value);
        }
    }
}
