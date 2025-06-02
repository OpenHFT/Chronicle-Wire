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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.JsonUtil;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * Test class to validate the serialization behavior of a map using JSONWire.
 * The keys for the map are integers, which is a special case because in JSON, keys must be strings.
 * This test is designed to ensure the keys are correctly serialized as strings.
 * It extends WireTestCommon for utility behaviors related to Wire tests.
 */
public class Issue328Test extends WireTestCommon {

    /**
     * Tests the serialization of a map where the keys are integers and values are their string representations.
     */
    @Test
    public void map() {
        // Initializes a wire with JSON format and types set to true.
        final Wire wire = new JSONWire().useTypes(true);
        final int size = 3;

        // Creates a map with keys as integers and values as their string representations.
        final Map<Integer, String> map = IntStream.range(0, size)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> Integer.toString(i)));

        // Writes the map to the wire.
        wire.getValueOut().object(map);

        // Retrieves the serialized output as a string.
        final String actual = wire.toString();

        // Constructs the expected serialized output.
        final String expected = IntStream.range(0, size)
                .boxed()
                .map(i -> String.format("\"%d\":\"%d\"", i, i))
                .collect(Collectors.joining(",", "{", "}"));

        // Print the actual serialized output for manual inspection.
        System.out.println("actual = " + actual);

        // Ensures that the serialized JSON has balanced brackets.
        JsonUtil.assertBalancedBrackets(actual);

        // Checks that the actual serialized output matches the expected one.
        assertEquals(expected, actual);
    }
}
