/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

// Use the Parameterized runner for JUnit to execute tests with different combinations of parameters
@RunWith(value = Parameterized.class)
public class PrimArraysTest extends WireTestCommon {

    // Class variables to hold the parameters
    private final WireType wireType;
    private final Object array;
    private final String asText;

    // Constructor that initializes the class variables
    public PrimArraysTest(WireType wireType, Object array, String asText) {
        this.wireType = wireType;
        this.array = array;
        this.asText = asText;
    }

    // Define the combinations of parameters with which the test method will be executed
    @NotNull
    @Parameterized.Parameters(name = "wt={0}, asText={2}")
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
    @Test
    public void testPrimArray() {
        Wire wire = createWire();  // Create a wire instance based on the wireType
        try {
            // Write the test array to the wire
            wire.write("test")
                    .object(array);
           // System.out.println(wire);
            // Assert that the textual representation matches when using WireType.TEXT
            if (wireType == WireType.TEXT)
                assertEquals(asText.trim(), wire.toString().trim());

            // Read the array from the wire and assert it matches the original
            @Nullable Object array2 = wire.read().object();
            assertEquals(array.getClass(), array2.getClass());
            assertEquals(Array.getLength(array), Array.getLength(array));
            for (int i = 0, len = Array.getLength(array); i < len; i++)
                assertEquals(Array.get(array, i), Array.get(array2, i));
        } finally {
            wire.bytes().releaseLast();  // Clean up resources
        }
    }

    // Helper method to create a wire instance
    private Wire createWire() {
        return wireType.apply(Bytes.elasticByteBuffer());
    }
}
