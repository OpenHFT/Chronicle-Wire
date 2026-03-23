/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryWireHighCodeTest extends WireTestCommon {

    // Test if the BinaryWireHighCode values are unique and no duplicates exist
    @Test
    public void testUnique() throws IllegalAccessException {
        // Ensure there are no pre-defined values
        assertEquals(0, BinaryWireHighCode.values().length);

        // Create a set to store the values
        Set<Integer> values = new HashSet<>();

        // Loop through each field in the BinaryWireHighCode class
        for (Field field : BinaryWireHighCode.class.getFields()) {
            // Retrieve the integer value of the current field
            int value = (Integer) field.get(null);

            // Add the value to the set and ensure it's unique (not already present)
            assertTrue(values.add(value));
        }
    }
}
