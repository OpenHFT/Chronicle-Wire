/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * Test suite for validating the marshalling capabilities of maps.
 * Inherits from WireTestCommon for common test setup and teardown functionalities.
 */
public class MapMarshallableTest extends WireTestCommon {

    /**
     * Test the process of copying values between maps and DTO objects.
     * The test covers:
     * - Copying values from a map to a DTO object.
     * - Copying values from a DTO object to a map.
     * - Copying values between maps with different implementations.
     */
    @Test
    public void test() {
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
        assertEquals(10, result.one);
        assertEquals(20, result.two);
        assertEquals(30, result.three);

        // Copy values from the MyDto instance back to a new LinkedHashMap
        @NotNull Map<String, Object> map2 = Wires.copyTo(result, new LinkedHashMap<>());
        // Validate the copied values
        assertEquals("{one=10, two=20, three=30}", map2.toString());

        // Copy values from the original map to a TreeMap (sorted map)
        @NotNull Map<String, Object> map3 = Wires.copyTo(map, new TreeMap<>());
        // Validate the copied values (the order may change due to the TreeMap sorting)
        assertEquals("{one=10, three=30, two=20}", map3.toString());
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
    }
}
