/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"rawtypes", "unchecked"})
final class WireMapTestSupport {
    private WireMapTestSupport() {
    }

    static void writeAndReadIntegerMap(Wire wire, Map<Integer, Integer> expected) {
        wire.writeDocument(false, c -> c.write(() -> "example").marshallable(expected));

        @NotNull final Map<Integer, Integer> actual = new HashMap<>();
        wire.readDocument(null, c -> {
            @Nullable Map m = c.read(() -> "example").marshallableAsMap(Integer.class, Integer.class, actual);
            assertEquals(m,
                    expected,
                    "Integer map should round trip from wire");
        });
    }

    static void assertMapInMap(String yaml) {
        Map<String, Object> fromString = Marshallable.fromString(yaml);
        assertEquals("{WithMap={innerMap={AUDUSD=AUDUSD1, USDPLN=USDPLN1}}}",
                fromString.toString(),
                "Map within map should match expected string");
    }

    static void assertMapWithQuestionMarks(String yaml) {
        Map<String, Object> fromString = Marshallable.fromString(yaml);
        assertEquals("{WithMap={innerMap={AUDUSD=AUDUSD1, USDPLN=USDPLN1}}}",
                fromString.toString(),
                "Map with question marks should match expected string");
    }

    static boolean writeAndReadStringMap(Function<Bytes<?>, Wire> wireFactory) {
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        try {
            @NotNull final Map<String, String> expected = new LinkedHashMap<>();
            expected.put("hello", "world");
            expected.put("hello1", "world1");
            expected.put("hello2", "world2");

            @NotNull final Wire wire = wireFactory.apply(bytes);

            wire.writeDocument(false, o -> o.writeEventName(() -> "example").map(expected));

            assertEquals("--- !!data\n" +
                            "example: {\n" +
                            "  hello: world,\n" +
                            "  hello1: world1,\n" +
                            "  hello2: world2\n" +
                            "}\n",
                    Wires.fromSizePrefixedBlobs(bytes),
                    "String map serialisation should match expected text");
            @NotNull final Map<String, String> actual = new LinkedHashMap<>();
            wire.readDocument(null, c -> c.read(() -> "example").marshallableAsMap(String.class, String.class, actual));
            assertEquals(expected,
                    actual,
                    "String map should round trip to expected values");
            return true;
        } finally {
            bytes.releaseLast();
        }
    }

    static void assertMarshallableMap(Function<Bytes<?>, Wire> wireFactory) {
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = wireFactory.apply(bytes);

        @NotNull final Map<MyMarshallable, MyMarshallable> expected = new LinkedHashMap<>();
        expected.put(new MyMarshallable("aKey"), new MyMarshallable("aValue"));
        expected.put(new MyMarshallable("aKey2"), new MyMarshallable("aValue2"));

        wire.writeDocument(false, o -> o.write(() -> "example")
                .marshallable(expected, MyMarshallable.class, MyMarshallable.class, true));

        assertEquals("--- !!data\n" +
                        "example: {\n" +
                        "  ? { MyField: aKey }: { MyField: aValue },\n" +
                        "  ? { MyField: aKey2 }: { MyField: aValue2 }\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes),
                "Marshallable map serialisation should match expected text");

        @NotNull final Map<MyMarshallable, MyMarshallable> actual = new LinkedHashMap<>();

        wire.readDocument(null, c -> c.read(() -> "example")
                .marshallableAsMap(
                        MyMarshallable.class,
                        MyMarshallable.class,
                        actual));

        assertEquals(expected,
                actual,
                "Marshallable map should round trip to expected values");

        wire.bytes().releaseLast();
    }

    static void assertObjectWithTreeMap(Function<Bytes<?>, Wire> wireFactory) {
        @NotNull Wire wire = wireFactory.apply(allocateElasticOnHeap());
        ObjectWithTreeMap value = new ObjectWithTreeMap();
        value.map.put("hello", "world");
        wire.write().object(value);

        ObjectWithTreeMap value2 = new ObjectWithTreeMap();
        wire.read().object(value2, ObjectWithTreeMap.class);
        assertEquals("{hello=world}",
                value2.map.toString(),
                "Tree map should round trip for typed object");

        wire.bytes().readPosition(0);
        ObjectWithTreeMap value3 = new ObjectWithTreeMap();
        wire.read().object(value3, Object.class);
        assertEquals("{hello=world}",
                value3.map.toString(),
                "Tree map should round trip for Object.class read");

        wire.bytes().readPosition(0);
        ObjectWithTreeMap value4 = wire.read().object(ObjectWithTreeMap.class);
        assertEquals("{hello=world}",
                value4.map.toString(),
                "Tree map should round trip for direct read");
    }
}
