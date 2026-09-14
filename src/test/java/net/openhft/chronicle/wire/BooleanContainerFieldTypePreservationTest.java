/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(Parameterized.class)
public class BooleanContainerFieldTypePreservationTest extends WireTestCommon {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> formats() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.YAML_ONLY},
                {WireType.JSON}
        });
    }

    @Parameterized.Parameter
    public WireType wireType;

    @Test
    public void setFieldDoesNotCollapseDistinctScalarTypes() {
        SetValues expected = new SetValues();
        expected.values = new LinkedHashSet<>(mixedValues());

        SetValues actual = roundTrip(expected, SetValues.class);
        assertFourDistinctValues(actual.values);
    }

    @Test
    public void handWrittenSetInputPreservesAllFourValues() {
        // Read independently of the writer, with separator whitespace for TextWire.
        String input = "{\"values\": [\"true\", true, \"false\", false]}";
        Bytes<?> bytes = Bytes.from(input);
        try {
            SetValues actual = wireType.apply(bytes).getValueIn().object(SetValues.class);
            assertNotNull(wireType.name(), actual);
            assertFourDistinctValues(actual.values);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void objectArrayFieldRetainsScalarTypes() {
        ArrayValues expected = new ArrayValues();
        expected.values = new Object[]{"true", true, "false", false};
        expected.afterText = "true";
        expected.afterFlag = false;

        ArrayValues actual = roundTrip(expected, ArrayValues.class);
        assertNotNull(wireType.name(), actual.values);
        assertArrayEquals(wireType.name(), expected.values, actual.values);
        for (int i = 0; i < expected.values.length; i++)
            assertSame(wireType + " values[" + i + "]",
                    expected.values[i].getClass(), actual.values[i].getClass());
        assertEquals(wireType.name(), "true", actual.afterText);
        assertEquals(wireType.name(), Boolean.FALSE, actual.afterFlag);
    }

    @Test
    public void mapOfListsFieldRetainsScalarTypes() {
        MapOfLists expected = new MapOfLists();
        expected.groups = new LinkedHashMap<>();
        expected.groups.put("first", mixedValues());
        expected.groups.put("second", Arrays.<Object>asList(false, "false", true, "true"));

        MapOfLists actual = roundTrip(expected, MapOfLists.class);
        assertEquals(wireType.name(), expected.groups, actual.groups);
    }

    @Test
    public void listOfMapsFieldRetainsScalarTypes() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("text", "true");
        first.put("flag", true);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("flag", false);
        second.put("text", "false");
        ListOfMaps expected = new ListOfMaps();
        expected.rows = Arrays.asList(first, second);

        ListOfMaps actual = roundTrip(expected, ListOfMaps.class);
        assertEquals(wireType.name(), expected.rows, actual.rows);
    }

    private void assertFourDistinctValues(Set<Object> actual) {
        assertNotNull(wireType.name(), actual);
        assertEquals(wireType.name(), 4, actual.size());
        assertEquals(wireType.name(), new LinkedHashSet<>(mixedValues()), actual);
    }

    private static List<Object> mixedValues() {
        return Arrays.<Object>asList("true", true, "false", false);
    }

    private <T extends SelfDescribingMarshallable> T roundTrip(T expected, Class<T> type) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            wireType.apply(bytes).getValueOut().marshallable(expected);
            // Decode into a fresh DTO: expected values must not seed field inference.
            T actual = wireType.apply(bytes).getValueIn().object(type);
            assertNotNull(wireType.name(), actual);
            return actual;
        } finally {
            bytes.releaseLast();
        }
    }

    public static class SetValues extends SelfDescribingMarshallable {
        public Set<Object> values;
    }

    public static class ArrayValues extends SelfDescribingMarshallable {
        public Object[] values;
        public Object afterText;
        public Object afterFlag;
    }

    public static class MapOfLists extends SelfDescribingMarshallable {
        public Map<String, List<Object>> groups;
    }

    public static class ListOfMaps extends SelfDescribingMarshallable {
        public List<Map<String, Object>> rows;
    }
}
