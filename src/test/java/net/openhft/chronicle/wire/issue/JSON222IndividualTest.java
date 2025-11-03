/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JSON222IndividualTest extends WireTestCommon {

    // Test empty JSON object representation
    @Test
    public void testEmptyBrackets() {
        checkSerialized("{}", new LinkedHashMap<>());
    }

    // Test JSON string representation with a tab character
    @Test
    public void testTab() {
        checkSerialized("\"hello\\tworld\"\n", "hello\tworld");
        checkDeserialized("hello\tworld", "\"hello\\tworld\"");
    }

    // Test JSON string representation with a special unicode character
    @Test
    public void testSpecial() {
        checkSerialized("\"\\u1000\"\n", "\u1000");
        checkDeserialized("\u1000", "\"\\u1000\"");
    }

    // Test nested JSON arrays
    @Test
    public void nestedSeq() {
        @SuppressWarnings("rawtypes")
        @NotNull List list = Arrays.asList(3L, Arrays.asList(4L));
        checkSerialized("[\n" +
                "  3,\n" +
                "  [\n" +
                "    4\n" +
                "  ]\n" +
                "]\n", list);
    }

    // Test parsing of a JSON object with array as key
    @Test
    public void parseArrayKey() {
        checkDeserialized("{5=[6], [7]=}", "{ '5': [ 6 ], [ 7 ] }\n");
    }

    // Helper method to check serialization of an object into JSON representation
    private void checkSerialized(@NotNull String expected, Object o) {
        @NotNull Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        try {
            wire.getValueOut()
                    .object(o);

            // Validate the serialized result with an external YAML parser
            try {
                @NotNull Yaml yaml = new Yaml();
                yaml.load(new StringReader(expected));
            } catch (Exception e) {
                throw e;
            }

            assertEquals(expected, wire.toString());

        } finally {
            // Release resources to prevent memory leaks
            wire.bytes().releaseLast();
        }
    }

    // Helper method to check deserialization of a JSON input into an object representation
    private void checkDeserialized(String expected, @NotNull String input) {
        @NotNull Wire wire = TextWire.from(input);

        // Validate the input with an external YAML parser
        try {
            @NotNull Yaml yaml = new Yaml();
            Object o = yaml.load(new StringReader(input));
           // Debugging output commented out
           // System.out.println(o);
        } catch (Exception e) {
            throw e;
        }

        // Deserialize the input and validate its string representation against the expected value
        @Nullable Object o = wire.getValueIn()
                .object();

        assertEquals(expected, o.toString());

        // Release resources to prevent memory leaks
        wire.bytes().releaseLast();
    }
}
