/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.JsonUtil;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to validate the serialization behavior of a map using JSONWire.
 * The keys for the map are integers, which is a special case because in JSON, keys must be strings.
 * This test is designed to ensure the keys are correctly serialized as strings.
 * It extends WireTestCommon for utility behaviors related to Wire tests.
 */
class Issue328Test extends WireTestCommon {

    /**
     * Tests the serialization of a map where the keys are integers and values are their string representations.
     */
    @Test
    void map() {
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
