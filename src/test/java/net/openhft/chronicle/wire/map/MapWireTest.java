/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for validating Map serialization and deserialization using different wire types.
 * Inherits from WireTestCommon for common test setup and teardown functionalities.
 */
class MapWireTest extends WireTestCommon {

    // The wire type for serialization and deserialization (TEXT or BINARY)
    private WireType wireType;

    // The map to be tested
    @SuppressWarnings("rawtypes")
    private Map m;

    /**
     * Provides a collection of test parameters including wire types and maps with various content.
     *
     * @return Collection of object arrays with wire types and maps.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        @NotNull WireType[] wireTypes = {WireType.TEXT, WireType.BINARY};
        for (WireType wt : wireTypes) {
            char maxValue = 256; // Character.MAX_VALUE;
            for (int i = 0; i < maxValue; i += 16) {
                @NotNull Map<Integer, String> map = new LinkedHashMap<>();
                for (int ch = i; ch < i + 16; ch++) {
                    if (Character.isValidCodePoint(ch)) {
                        final String s = Character.toString((char) ch);
                        map.put(i, s);
                    }
                }
                @NotNull Object[] test = {wt, map};
                list.add(test);
            }
        }
        return list;
    }

    /**
     * Test the serialization and deserialization of maps using the given wire type.
     * The test will serialize the map into wire format, then deserialize it back
     * and compare to the original map to ensure data integrity.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @ParameterizedTest
    @MethodSource("combinations")
    void writeMap(WireType wireType, Map m) {
        this.wireType = wireType;
        this.m = m;
        // Create an elastic buffer to hold serialized data
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Apply the wire type to the bytes buffer
        Wire wire = wireType.apply(bytes);
        // Serialize the map into the wire format
        wire.getValueOut().marshallable(m);
        // Uncomment the following line to print the wire content
        // System.out.println(wire);

        // Deserialize the map from the wire format
        @Nullable Map m2 = wire.getValueIn().marshallableAsMap(Object.class, Object.class);

        // Ensure that the deserialized map matches the original map
        assertEquals(m, m2);

        // Release the bytes buffer
        bytes.releaseLast();
    }
}
