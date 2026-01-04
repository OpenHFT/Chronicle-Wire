/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CopyTest extends WireTestCommon {

    // Define the combinations of wire types and settings for the test
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                // new Object[] {WireType.TEXT, WireType.BINARY, true}, // not supported yet
                // new Object[] {WireType.TEXT, WireType.BINARY, false}, // not supported yet
                new Object[]{WireType.YAML, WireType.BINARY_LIGHT, true},
                // new Object[]{WireType.TEXT, WireType.BINARY_LIGHT, true},
                new Object[]{WireType.BINARY, WireType.JSON, false},
                new Object[]{WireType.BINARY, WireType.TEXT, true},
                new Object[]{WireType.BINARY, WireType.TEXT, false},
                //  new Object[]{WireType.RAW, WireType.RAW, false},
                new Object[]{WireType.JSON, WireType.JSON, false},
                // new Object[]{WireType.JSON, WireType.JSON, true}, // not supported as types are dropped for backward compatability
                new Object[]{WireType.JSON, WireType.JSON_ONLY, false},
                new Object[]{WireType.JSON_ONLY, WireType.JSON_ONLY, false},
                new Object[]{WireType.JSON_ONLY, WireType.JSON_ONLY, true},
                new Object[]{WireType.TEXT, WireType.TEXT, false},
                new Object[]{WireType.TEXT, WireType.TEXT, true},
                new Object[]{WireType.YAML, WireType.YAML, false},
                new Object[]{WireType.YAML, WireType.YAML, true},
                new Object[]{WireType.YAML_ONLY, WireType.YAML_ONLY, true},
                new Object[]{WireType.JSON_ONLY, WireType.TEXT, true},
                new Object[]{WireType.JSON_ONLY, WireType.YAML, true},
                new Object[]{WireType.JSON_ONLY, WireType.BINARY, true},
                new Object[]{WireType.JSON_ONLY, WireType.BINARY_LIGHT, true}
        );
    }

    @MethodSource("wireTypes")
    @SuppressWarnings("rawtypes")
    @ParameterizedTest(name = "from: {0}, to: {1}, withType: {2}")
    @DisplayName("Copies wire data across formats with optional type metadata")
    void testCopy(WireType from, WireType to, boolean withType) {
        // Create source bytes and wire objects
        Bytes<?> bytesFrom = Bytes.allocateElasticOnHeap(64);
        Wire wireFrom = from.apply(bytesFrom);

        // Create destination bytes and wire objects
        Bytes<?> bytesTo = Bytes.allocateElasticOnHeap(64);
        Wire wireTo = to.apply(bytesTo);

        // Create an instance of 'AClass' for testing
        AClass a = create();

        // Write the 'AClass' instance to the source wire
        if (withType)
            wireFrom.write("test").object(a);
        else
            wireFrom.write("test").marshallable(a);

        // Copy data from source to destination wire
        wireFrom.copyTo(wireTo);

        // Perform checks if the destination wire type is JSON
        if (to == WireType.JSON || to == WireType.JSON_ONLY) {
            final String text = wireTo.toString();
            assertFalse(text.contains("? "),
                    "JSON output should not include type tags, from=" + from + ", to=" + to + ", withType=" + withType + ", text=" + text);
            assertFalse(text.contains("\n\""),
                    "JSON output should not include raw newline quotes, from=" + from + ", to=" + to + ", withType=" + withType + ", text=" + text);
        }

        if (to == WireType.BINARY_LIGHT)
            wireTo.readingDocument();
        // Validate the data in the destination wire
        final String event = wireTo.readEvent(String.class);
        assertEquals("test", event,
                "Copied event name should be preserved, from=" + from + ", to=" + to + ", withType=" + withType);
        AClass b = wireTo.getValueIn().object(AClass.class);

        assertEquals(a, b,
                "Copied object should match original, from=" + from + ", to=" + to + ", withType=" + withType);
        assertEquals(a.map, b.map,
                "Copied map should match original, from=" + from + ", to=" + to + ", withType=" + withType);
        assertArrayEquals(a.array, b.array,
                "Copied array should match original, from=" + from + ", to=" + to + ", withType=" + withType);
        assertEquals(a.intValue, b.intValue,
                "Copied intValue should match original, from=" + from + ", to=" + to + ", withType=" + withType);
        assertEquals(a.value, b.value, 0.0,
                "Copied value should match original, from=" + from + ", to=" + to + ", withType=" + withType);

        // If testing with type information, re-run copy with typedMarshallable
        if (withType) {
            wireFrom.clear();
            wireTo.clear();

            wireFrom.write("msg").typedMarshallable(a);
            wireFrom.copyTo(wireTo);
            if (from == WireType.JSON_ONLY) {
                System.out.println(wireFrom);
                System.out.println(wireTo);
            }
            if (to == WireType.BINARY_LIGHT)
                wireTo.readingDocument();
            Object b2 = wireTo.read("msg").object();

            assertEquals(a, b2,
                    "Typed marshallable copy should match original, from=" + from + ", to=" + to + ", withType=" + withType);
        }
    }

    // Helper method to create a test instance of 'AClass'
    private AClass create() {
        AClass aClass = new AClass();
        aClass.map = new EnumMap<>(CcyPair.class);
        aClass.map.put(CcyPair.EURUSD, "eurusd");
        aClass.array = new String[]{"hello", "there"};
        aClass.intValue = 11;
        aClass.value = 123.4;
        return aClass;
    }

    // Class representing the data structure to be used in the copy test
    @SuppressWarnings("unused")
    static class AClass extends SelfDescribingMarshallable {
        Map<CcyPair, String> map;
        String[] array;
        int intValue;
        double value;
    }
}
