/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class NullScalarSemanticsTest extends WireTestCommon {
    private static final WireType[] YAML_TYPES = {WireType.TEXT, WireType.YAML, WireType.YAML_ONLY};
    private static final WireType[] TYPES = {WireType.TEXT, WireType.YAML, WireType.YAML_ONLY, WireType.JSON, WireType.JSON_ONLY};

    public static class Value extends SelfDescribingMarshallable {
        public List<String> values;
        public int next;
    }

    public static class Nested extends SelfDescribingMarshallable {
        public Value value;
        public int next;
    }

    @Test
    public void nullCollectionShapesSurviveRoundTrip() {
        for (WireType type : TYPES) {
            for (List<String> values : Arrays.<List<String>>asList(null, Collections.emptyList(),
                    Collections.singletonList(null), Arrays.asList(null, "x"))) {
                Value original = new Value(); original.values = values; original.next = 7;
                Value actual = type.fromString(Value.class, type.asString(original));
                assertEquals(type.toString(), values, actual.values);
                assertEquals(7, actual.next);
            }
        }
    }

    @Test
    public void nullAndStringElementsRemainDistinct() {
        for (WireType type : TYPES) {
            Value value = type.fromString(Value.class, "{\"values\":[null,\"null\",\"\",\"x\"],\"next\":7}");
            assertEquals(type.toString(), Arrays.asList(null, "null", "", "x"), value.values);
            assertEquals(7, value.next);
            assertNull(type.fromString(Value.class, "{\"values\":null,\"next\":7}").values);
            assertEquals(Collections.emptyList(), type.fromString(Value.class, "{\"values\":[],\"next\":7}").values);
        }
    }

    @Test
    public void yamlCoreNullSpellingsAndTagsResolveAsNull() {
        for (WireType type : YAML_TYPES) {
            for (String scalar : Arrays.asList("null", "Null", "NULL", "~", "!!null \"\"", "!!null null")) {
                Value value = type.fromString(Value.class, "values: " + scalar + "\nnext: 7\n");
                assertNull(type + " " + scalar, value.values);
                assertEquals(7, value.next);
                value = type.fromString(Value.class, "values: [" + scalar + "]\nnext: 7\n");
                assertEquals(type + " " + scalar, Collections.singletonList(null), value.values);
                assertEquals(7, value.next);
            }
        }
    }

    @Test
    public void stringsRoundTripWithoutBecomingNull() {
        List<String> strings = Arrays.asList("null", "Null", "NULL", "NuLl", "~", "", "nullish", "null value", "null#suffix");
        for (WireType type : TYPES) {
            Value value = new Value(); value.values = strings; value.next = 7;
            Value actual = type.fromString(Value.class, type.asString(value));
            assertEquals(type.toString(), strings, actual.values);
            assertEquals(7, actual.next);
        }
    }

    @Test
    public void yamlStringsRetainTheirTypesInAnotherParser() {
        List<String> strings = Arrays.asList("null", "Null", "NULL", "~", "");
        for (WireType type : YAML_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                Value value = new Value(); value.values = strings; value.next = 7;
                value.writeMarshallable(type.apply(bytes));
                Map<?, ?> parsed = new Yaml().load(bytes.toString());
                assertEquals(type + " " + bytes, strings, parsed.get("values"));
                assertEquals(7, parsed.get("next"));
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void emptyYamlMappingValuePreservesTheFollowingKey() {
        for (WireType type : YAML_TYPES) {
            for (String separator : Arrays.asList("\n", " # empty\n", "\r\n", "\n\n")) {
                Value value = type.fromString(Value.class, "values:" + separator + "next: 7\n");
                assertNull(type.toString(), value.values);
                assertEquals(type.toString(), 7, value.next);
            }
            Value last = type.fromString(Value.class, "next: 7\nvalues:\n");
            assertNull(type.toString(), last.values);
            assertEquals(7, last.next);
            for (String input : Arrays.asList("a:\nb:\n", "a:\nb:", "{ a: , b: }")) {
                Map<?, ?> map = type.fromString(Map.class, input);
                assertEquals(type.toString(), 2, map.size());
                assertTrue(map.containsKey("a"));
                assertTrue(map.containsKey("b"));
                assertNull(map.get("a"));
                assertNull(map.get("b"));
            }
        }
    }

    @Test
    public void emptyBlockSequenceEntriesPreserveTheirNeighbours() {
        for (WireType type : new WireType[]{WireType.YAML, WireType.YAML_ONLY}) {
            for (String input : Arrays.asList("values:\n  -\n  - x\n  - # empty\n  -\nnext: 7\n",
                    "values:\n-\n- x\n- # empty\n-\nnext: 7\n")) {
                Value value = type.fromString(Value.class, input);
                assertEquals(type.toString(), Arrays.asList(null, "x", null, null), value.values);
                assertEquals(7, value.next);
            }
        }
    }

    @Test
    public void nullResolutionAlsoAppliesToUntypedAndNestedValues() {
        for (WireType type : TYPES) {
            Map<?, ?> value = type.fromString(Map.class, "{\"values\":[null,\"null\"],\"nested\":{\"value\":null},\"next\":7}");
            assertEquals(type.toString(), Arrays.asList(null, "null"), value.get("values"));
            assertTrue(((Map<?, ?>) value.get("nested")).containsKey("value"));
            assertNull(((Map<?, ?>) value.get("nested")).get("value"));
            assertEquals(7L, ((Number) value.get("next")).longValue());
            Nested nested = type.fromString(Nested.class, "{\"value\":null,\"next\":7}");
            assertNull(nested.value);
            assertEquals(7, nested.next);
        }
    }

    @Test
    public void nullDoesNotMatchTheStartOfAPlainScalar() {
        for (WireType type : TYPES) {
            for (String text : Arrays.asList("nullish", "null value", "~suffix")) {
                Value value = type.fromString(Value.class, "values: " + text + "\nnext: 7\n");
                assertEquals(type + " " + text, Collections.singletonList(text), value.values);
                assertEquals(7, value.next);
            }
        }
    }

    @Test
    public void explicitStringTagSuppressesNullResolution() {
        for (WireType type : YAML_TYPES) {
            Value value = type.fromString(Value.class, "values: [!!str null, !!str ~, 'null', \"~\"]\nnext: 7\n");
            assertEquals(type.toString(), Arrays.asList("null", "~", "null", "~"), value.values);
            assertEquals(7, value.next);
            Map<?, ?> untyped = type.fromString(Map.class, "value: !!str null\nnext: 7\n");
            assertEquals(type.toString(), "null", untyped.get("value"));
        }
    }

    @Test
    public void bufferedNullReadClearsStaleItemsAndPreservesNextField() {
        for (WireType type : TYPES) {
            Bytes<?> bytes = Bytes.from("values: null, next: 7");
            try {
                Wire wire = type.apply(bytes);
                List<String> values = new ArrayList<>(Collections.singletonList("old"));
                assertFalse(type.toString(), wire.read("values").sequence(values, new ArrayList<>(), () -> "unused", new ValueIn.Reader() {
                    @Override
                    public <T> void accept(ValueIn in, List<T> list, List<T> buffer, Supplier<T> supplier) {
                        fail("null has no elements");
                    }
                }));
                assertTrue(type.toString(), values.isEmpty());
                assertEquals(7, wire.read("next").int32());
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void textDestinationsDistinguishNullFromQuotedNull() {
        for (WireType type : TYPES) {
            Bytes<?> bytes = Bytes.from("a: null, b: \"null\", c: null, d: \"null\", next: 7");
            Bytes<?> target = Bytes.allocateElasticOnHeap();
            try {
                Wire wire = type.apply(bytes);
                StringBuilder sb = new StringBuilder("old");
                assertNull(type.toString(), wire.read("a").textTo(sb));
                assertEquals("null", wire.read("b").textTo(sb).toString());
                target.append("old");
                assertNull(type.toString(), wire.read("c").textTo(target));
                assertEquals("null", wire.read("d").textTo(target).toString());
                assertEquals(7, wire.read("next").int32());
            } finally {
                target.releaseLast();
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void yamlNullAndStringKeysRemainDistinct() {
        for (WireType type : new WireType[]{WireType.YAML, WireType.YAML_ONLY}) {
            Map<?, ?> parsed = type.fromString(Map.class, "null: null\n\"null\": \"null\"\nnext: done\n");
            assertEquals(3, parsed.size());
            assertTrue(parsed.containsKey(null));
            assertNull(parsed.get(null));
            assertEquals("null", parsed.get("null"));
            String written = type.asString(parsed);
            assertEquals(type + " " + written, parsed, type.fromString(Map.class, written));
            assertEquals(parsed, new Yaml().load(written));
        }
    }
}
