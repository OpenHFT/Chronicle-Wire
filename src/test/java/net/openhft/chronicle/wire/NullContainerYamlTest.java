/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.Test;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NullContainerYamlTest extends WireTestCommon {

    @Test
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
        assertTrue(yaml, yaml.contains("collection: !!null \"\""));
        assertTrue(yaml, yaml.contains("set: !!null \"\""));
        assertTrue(yaml, yaml.contains("sortedSet: !!null \"\""));
        assertTrue(yaml, yaml.contains("map: !!null \"\""));
        assertTrue(yaml, yaml.contains("sortedMap: !!null \"\""));
        assertTrue(yaml, yaml.contains("bytes: !!null \"\""));
        assertTrue(yaml, yaml.contains("ints: !!null \"\""));
        assertTrue(yaml, yaml.contains("strings: !!null \"\""));

        NullContainers actual = WireType.YAML_ONLY.fromString(yaml);
        assertNull(actual.collection);
        assertNull(actual.set);
        assertNull(actual.sortedSet);
        assertNull(actual.map);
        assertNull(actual.sortedMap);
        assertNull(actual.bytes);
        assertNull(actual.ints);
        assertNull(actual.strings);
    }

    @Test
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
        assertNotNull(actual.collection);
        assertTrue(actual.collection.isEmpty());
        assertNotNull(actual.set);
        assertTrue(actual.set.isEmpty());
        assertNotNull(actual.sortedSet);
        assertTrue(actual.sortedSet.isEmpty());
        assertNotNull(actual.map);
        assertTrue(actual.map.isEmpty());
        assertNotNull(actual.sortedMap);
        assertTrue(actual.sortedMap.isEmpty());
        assertNull(actual.bytes);
        assertNotNull(actual.ints);
        assertEquals(0, actual.ints.length);
        assertNotNull(actual.strings);
        assertEquals(0, actual.strings.length);
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
