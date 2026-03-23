/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for YAML serialisation of null and empty container fields.
 * Verifies that null collections, maps, and arrays round-trip correctly using
 * the !!null YAML tag, and that empty containers remain empty after deserialisation.
 */
@DisplayName("YAML Wire Format: null and empty container serialisation")
@SuppressWarnings({"checkstyle:MMOverusedWord", // "round-trip" is the standard serialisation term
        "checkstyle:MMLacksPurpose"}) // file has Javadoc and assertion messages explaining purpose
public class NullContainerYamlTest extends WireTestCommon {

    @Test
    @DisplayName("null container fields round-trip as null with !!null tag")
    @SuppressWarnings("deprecation") // testing deprecated WireType.fromString() intentionally
    public void nullFieldsRoundTripAsNull() {
        NullContainers expected = new NullContainers();
        expected.collection = null;
        expected.set = null;
        expected.sortedSet = null;
        expected.map = null;
        expected.sortedMap = null;
        expected.bytes = null;
        expected.ints = null;
        expected.strings = null;

        String yaml = WireType.YAML_ONLY.asString(expected);
        assertTrue(yaml.contains("collection: !!null \"\""),
                "YAML should contain !!null tag for collection field; actual:\n" + yaml);
        assertTrue(yaml.contains("set: !!null \"\""),
                "YAML should contain !!null tag for set field; actual:\n" + yaml);
        assertTrue(yaml.contains("sortedSet: !!null \"\""),
                "YAML should contain !!null tag for sortedSet field; actual:\n" + yaml);
        assertTrue(yaml.contains("map: !!null \"\""),
                "YAML should contain !!null tag for map field; actual:\n" + yaml);
        assertTrue(yaml.contains("sortedMap: !!null \"\""),
                "YAML should contain !!null tag for sortedMap field; actual:\n" + yaml);
        assertTrue(yaml.contains("bytes: !!null \"\""),
                "YAML should contain !!null tag for bytes field; actual:\n" + yaml);
        assertTrue(yaml.contains("ints: !!null \"\""),
                "YAML should contain !!null tag for ints field; actual:\n" + yaml);
        assertTrue(yaml.contains("strings: !!null \"\""),
                "YAML should contain !!null tag for strings field; actual:\n" + yaml);

        NullContainers actual = WireType.YAML_ONLY.fromString(yaml);
        assertNull(actual.collection, "collection should be null after round-trip");
        assertNull(actual.set, "set should be null after round-trip");
        assertNull(actual.sortedSet, "sortedSet should be null after round-trip");
        assertNull(actual.map, "map should be null after round-trip");
        assertNull(actual.sortedMap, "sortedMap should be null after round-trip");
        assertNull(actual.bytes, "bytes should be null after round-trip");
        assertNull(actual.ints, "ints should be null after round-trip");
        assertNull(actual.strings, "strings should be null after round-trip");
    }

    @Test
    @DisplayName("empty container fields round-trip as empty, not null")
    @SuppressWarnings("deprecation") // testing deprecated WireType.fromString() intentionally
    public void emptyFieldsRoundTripAsEmpty() {
        NullContainers expected = new NullContainers();
        expected.collection = new ArrayList<>();
        expected.set = new HashSet<>();
        expected.sortedSet = new TreeSet<>();
        expected.map = new LinkedHashMap<>();
        expected.sortedMap = new TreeMap<>();
        expected.bytes = null;
        expected.ints = new int[0];
        expected.strings = new String[0];

        String yaml = WireType.YAML_ONLY.asString(expected);
        NullContainers actual = WireType.YAML_ONLY.fromString(yaml);
        assertNotNull(actual.collection, "collection should not be null after round-trip");
        assertTrue(actual.collection.isEmpty(), "collection should be empty after round-trip");
        assertNotNull(actual.set, "set should not be null after round-trip");
        assertTrue(actual.set.isEmpty(), "set should be empty after round-trip");
        assertNotNull(actual.sortedSet, "sortedSet should not be null after round-trip");
        assertTrue(actual.sortedSet.isEmpty(), "sortedSet should be empty after round-trip");
        assertNotNull(actual.map, "map should not be null after round-trip");
        assertTrue(actual.map.isEmpty(), "map should be empty after round-trip");
        assertNotNull(actual.sortedMap, "sortedMap should not be null after round-trip");
        assertTrue(actual.sortedMap.isEmpty(), "sortedMap should be empty after round-trip");
        assertNull(actual.bytes, "bytes should remain null (was set to null)");
        assertNotNull(actual.ints, "ints array should not be null after round-trip");
        assertEquals(0, actual.ints.length, "ints array should have zero length after round-trip");
        assertNotNull(actual.strings, "strings array should not be null after round-trip");
        assertEquals(0, actual.strings.length, "strings array should have zero length after round-trip");
    }

    static final class NullContainers extends SelfDescribingMarshallable {
        Collection<String> collection;
        Set<Long> set;
        SortedSet<Integer> sortedSet;
        Map<String, Integer> map;
        SortedMap<String, Integer> sortedMap;
        byte[] bytes;
        int[] ints;
        String[] strings;
    }
}
