/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class CollectionNullSemanticsTest extends WireTestCommon {
    private static final WireType[] TYPES = {WireType.TEXT, WireType.YAML, WireType.YAML_ONLY, WireType.JSON, WireType.JSON_ONLY};
    public static class Value extends SelfDescribingMarshallable {
        public List<String> values;
    }

    public static class Child extends Value {
    }

    public static class Nested extends SelfDescribingMarshallable {
        public Value nested;
    }

    public static class WithStatic extends SelfDescribingMarshallable {
        public static List<String> shared;
        public int value;
    }

    public static class WithFinalStatic extends SelfDescribingMarshallable {
        public static final List<String> SHARED = Collections.singletonList(null);
        public int value;
    }

    @Test
    public void singletonNullIsData() {
        for (WireType type : TYPES) {
            Value value = type.fromString(Value.class, "{\"values\":[null]}");
            assertEquals(Collections.singletonList(null), value.values);
        }
    }

    @Test
    public void nullEmptyAndMixedListsRemainDistinct() {
        for (WireType type : TYPES) {
            assertNull(type.fromString(Value.class, "{\"values\":null}").values);
            assertEquals(Collections.emptyList(), type.fromString(Value.class, "{\"values\":[]}").values);
            assertEquals(Arrays.asList(null, "x"),
                    type.fromString(Value.class, "{\"values\":[null,\"x\"]}").values);
        }
    }

    @Test
    public void deserialisingDoesNotMutateStaticCollection() {
        List<String> original = WithStatic.shared;
        List<String> sentinel = Collections.singletonList(null);
        WithStatic.shared = sentinel;
        try {
            for (WireType type : TYPES) {
                assertEquals(7, type.fromString(WithStatic.class, "{\"value\":7}").value);
                assertSame(type.toString(), sentinel, WithStatic.shared);
            }
        } finally {
            WithStatic.shared = original;
        }
    }

    @Test
    public void finalStaticCollectionDoesNotPreventReading() {
        for (WireType type : TYPES) {
            assertEquals(7, type.fromString(WithFinalStatic.class, "{\"value\":7}").value);
            assertEquals(Collections.singletonList(null), WithFinalStatic.SHARED);
        }
    }

    @Test
    public void inheritedAndNestedNullCollectionsAreDecodedAtTheirValue() {
        for (WireType type : TYPES) {
            assertNull(type.fromString(Child.class, "{\"values\":null}").values);
            Nested nested = type.fromString(Nested.class, "{\"nested\":{\"values\":null}}");
            assertNotNull(nested.nested);
            assertNull(nested.nested.values);
        }
    }

    @Test
    public void directNullSequenceDoesNotInvokeReaderOrLoseNextField() {
        Bytes<?> bytes = Bytes.from("values: null, next: 7");
        try {
            Wire wire = new JSONWire(bytes);
            assertFalse(wire.read("values").sequence(new ArrayList<>(), (list, in) -> fail("null has no elements")));
            assertEquals(7, wire.read("next").int32());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void bufferedNullSequenceClearsListWithoutInvokingReader() {
        Bytes<?> bytes = Bytes.from("values: null, next: 7");
        List<String> list = new ArrayList<>(Collections.singletonList("old"));
        try {
            Wire wire = new JSONWire(bytes);
            assertFalse(wire.read("values").sequence(list, new ArrayList<>(), () -> "unused", new ValueIn.Reader() {
                @Override
                public <T> void accept(ValueIn in, List<T> target, List<T> buffer, Supplier<T> supplier) {
                    fail("null has no elements");
                }
            }));
            assertTrue(list.isEmpty());
            assertEquals(7, wire.read("next").int32());
        } finally {
            bytes.releaseLast();
        }
    }
}
