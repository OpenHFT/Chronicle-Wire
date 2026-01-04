/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test suite for validating the marshalling capabilities of maps.
 * Inherits from WireTestCommon for common test setup and teardown functionalities.
 */
class MapMarshallableTest extends WireTestCommon {
    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for map marshalling test");
    }

    /**
     * Test the process of copying values between maps and DTO objects.
     * The test covers:
     * - Copying values from a map to a DTO object.
     * - Copying values from a DTO object to a map.
     * - Copying values between maps with different implementations.
     */
    @Test
    @DisplayName("Copies map values into DTO and back")
    void test() {
        // Initialize a LinkedHashMap and populate it with sample data
        @NotNull final Map<String, Object> map = new LinkedHashMap<>();
        map.put("one", 10);
        map.put("two", 20);
        map.put("three", 30);

        // Create an instance of MyDto to be used in the copy operation
        @NotNull MyDto usingInstance = new MyDto();
        // Copy values from the map to the MyDto instance
        @NotNull MyDto result = Wires.copyTo(map, usingInstance);

        // Validate the values copied to the MyDto instance
        assertEquals(10, result.one, "DTO field one should match map value");
        assertEquals(20, result.two, "DTO field two should match map value");
        assertEquals(30, result.three, "DTO field three should match map value");

        // Copy values from the MyDto instance back to a new LinkedHashMap
        @NotNull Map<String, Object> map2 = Wires.copyTo(result, new LinkedHashMap<>());
        // Validate the copied values
        assertEquals("{one=10, two=20, three=30}", map2.toString(),
                "LinkedHashMap copy should preserve insertion order");

        // Copy values from the original map to a TreeMap (sorted map)
        @NotNull Map<String, Object> map3 = Wires.copyTo(map, new TreeMap<>());
        // Validate the copied values (the order may change due to the TreeMap sorting)
        assertEquals("{one=10, three=30, two=20}", map3.toString(),
                "TreeMap copy should reflect sorted order");
    }

    /**
     * Sample DTO class to be used in the marshalling tests.
     * Inherits from SelfDescribingMarshallable for marshalling capabilities.
     */
    private static class MyDto extends SelfDescribingMarshallable {
        // Fields corresponding to the keys in the test map
        int one;
        int two;
        int three;

        MyDto() {
            one = -1;
            two = -1;
            three = -1;
        }
    }
}
