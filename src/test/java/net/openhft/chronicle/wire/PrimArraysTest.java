/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Use the Parameterized runner for JUnit to execute tests with different combinations of parameters
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
class PrimArraysTest extends WireTestCommon {

    // Class variables to hold the parameters
    private WireType wireType;
    private Object array;
    private String asText;

    // Constructor that initializes the class variables
    void initPrimArraysTest(WireType wireType, Object array, String asText) {
        this.wireType = wireType;
        this.array = array;
        this.asText = asText;
    }

    // Define the combinations of parameters with which the test method will be executed
    @NotNull
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        for (WireType wt : new WireType[]{
                WireType.TEXT,
                WireType.BINARY,
                WireType.YAML
        }) {
            // Define array primitives and their textual representations
            @NotNull final Object[] objects = {
                    new boolean[]{true, false},
                    "test: !boolean[] [ true, false ]",
                    "test: !boolean[] [ ]",
                    new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE},
                    "test: !byte[] !!binary gAB/\n",
                    "test: !byte[] !!binary \n",
                    new char[]{Character.MIN_VALUE, '?', Character.MAX_VALUE},
                    "test: !char[] [ \"\\0\", \"?\", \"\\uFFFF\" ]",
                    "test: !char[] [ ]",
                    new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE},
                    "test: !short[] [ -32768, -1, 0, 1, 32767 ]",
                    "test: !short[] [ ]",
                    new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE},
                    "test: !int[] [ -2147483648, -1, 0, 1, 2147483647 ]",
                    "test: !int[] [ ]",
                    new long[]{Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE},
                    "test: !long[] [ -9223372036854775808, -1, 0, 1, 9223372036854775807 ]",
                    "test: !long[] [ ]",
                    new float[]{Float.MIN_VALUE, -1, 0, 1, Float.MAX_VALUE},
                    "test: !float[] [ 1.4E-45, -1.0, 0.0, 1.0, 3.4028235E38 ]",
                    "test: !float[] [ ]",
                    new double[]{Double.MIN_VALUE, -1, 0, 1, Double.MAX_VALUE},
                    "test: !double[] [ 4.9E-324, -1.0, 0.0, 1.0, 1.7976931348623157E308 ]",
                    "test: !double[] [ ]"
            };
            // Generate combinations based on the array and its representation
            for (int i = 0; i < objects.length; i += 3) {
                Object array = objects[i];
                list.add(new Object[]{wt, array, objects[i + 1]});
                final Object emptyArray = Array.newInstance(array.getClass().getComponentType(), 0);
                list.add(new Object[]{wt, emptyArray, objects[i + 2]});
            }
        }
        return list;  // Return the combinations
    }

    // The test method that will be executed for each combination of parameters
    @MethodSource("combinations")
    @ParameterizedTest(name = "wt={0}, asText={2}")
    @DisplayName("Primitive arrays round-trip across wire types")
    void testPrimArray(WireType wireType, Object array, String asText) {
        initPrimArraysTest(wireType, array, asText);
        Wire wire = createWire();  // Create a wire instance based on the wireType
        try {
            // Write the test array to the wire
            wire.write("test")
                    .object(array);
            // Assert that the textual representation matches when using WireType.TEXT
            if (wireType == WireType.TEXT) {
                assertEquals(asText.trim(), wire.toString().trim(),
                        "Text wire output should match the expected representation");
            }

            // Read the array from the wire and assert it matches the original
            @Nullable Object array2 = wire.read().object();
            assertEquals(array.getClass(), array2.getClass(), "Array type should round-trip");
            assertEquals(Array.getLength(array), Array.getLength(array2), "Array length should round-trip");
            for (int i = 0, len = Array.getLength(array); i < len; i++)
                assertEquals(Array.get(array, i), Array.get(array2, i), "Array element should round-trip at index " + i);
        } finally {
            wire.bytes().releaseLast();  // Clean up resources
        }
    }

    // Helper method to create a wire instance
    private Wire createWire() {
        return wireType.apply(Bytes.allocateElasticOnHeap());
    }
}
