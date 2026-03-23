/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that deserialising StringBuilder[] produces distinct StringBuilder instances
 * with correct values, rather than reusing the same instance across array elements.
 */
public class StringBuilderArrayTest extends WireTestCommon {

    private WireType wireType;

    @NotNull
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML_ONLY},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.JSON}
        );
    }

    public static class StringBuilderArrayDto extends SelfDescribingMarshallable {
        StringBuilder[] a;
        StringBuilder[] b;
    }

    /**
     * Tests that each element in a deserialised StringBuilder[] is a distinct instance
     * with the correct content, both within and across arrays.
     */
    @ParameterizedTest
    @MethodSource("wireTypes")
    public void testStringBuilderArrayDistinctElements(WireType wireType) {
        this.wireType = wireType;
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);

        StringBuilderArrayDto dto = new StringBuilderArrayDto();
        dto.a = new StringBuilder[]{new StringBuilder("baz"), new StringBuilder("qux")};
        dto.b = new StringBuilder[]{new StringBuilder("waldo"), new StringBuilder("bar")};

        wire.write("data").object(dto);
        StringBuilderArrayDto read = wire.read("data").object(StringBuilderArrayDto.class);

        // Each element within an array should have distinct content
        assertEquals("baz", read.a[0].toString());
        assertEquals("qux", read.a[1].toString());
        assertEquals("waldo", read.b[0].toString());
        assertEquals("bar", read.b[1].toString());

        // Elements should be distinct StringBuilder instances, not the same object
        assertNotSame(read.a[0], read.a[1],
                "Elements within array 'a' should be distinct instances");
        assertNotSame(read.b[0], read.b[1],
                "Elements within array 'b' should be distinct instances");
        assertNotSame(read.a[0], read.b[0],
                "Elements across arrays 'a' and 'b' should be distinct instances");

        bytes.releaseLast();
    }

    /**
     * Tests that null elements within a StringBuilder[] are deserialised as null
     * rather than as empty or stale StringBuilder instances.
     */
    @ParameterizedTest
    @MethodSource("wireTypes")
    public void testStringBuilderArrayWithNullElements(WireType wireType) {
        this.wireType = wireType;
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);

        StringBuilderArrayDto dto = new StringBuilderArrayDto();
        dto.a = new StringBuilder[]{new StringBuilder("hello"), null, new StringBuilder("world")};
        dto.b = null;

        wire.write("data").object(dto);
        StringBuilderArrayDto read = wire.read("data").object(StringBuilderArrayDto.class);

        assertEquals("hello", read.a[0].toString());
        assertNull(read.a[1], "Null element should deserialise as null");
        assertEquals("world", read.a[2].toString());
        assertNotSame(read.a[0], read.a[2]);
        assertNull(read.b, "Null array should deserialise as null");

        bytes.releaseLast();
    }

    /**
     * Tests that an empty StringBuilder[] round-trips correctly.
     */
    @ParameterizedTest
    @MethodSource("wireTypes")
    public void testEmptyStringBuilderArray(WireType wireType) {
        this.wireType = wireType;
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);

        StringBuilderArrayDto dto = new StringBuilderArrayDto();
        dto.a = new StringBuilder[0];
        dto.b = new StringBuilder[0];

        wire.write("data").object(dto);
        StringBuilderArrayDto read = wire.read("data").object(StringBuilderArrayDto.class);

        assertNotNull(read.a);
        assertEquals(0, read.a.length);
        assertNotNull(read.b);
        assertEquals(0, read.b.length);

        bytes.releaseLast();
    }

    /**
     * Tests that the reuse path (o != null) still works correctly when an existing
     * StringBuilder instance is provided.
     */
    @ParameterizedTest
    @MethodSource("wireTypes")
    public void testStringBuilderFieldReuse(WireType wireType) {
        this.wireType = wireType;
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);

        wire.write("value").text("hello");
        bytes.readPosition(0);

        StringBuilder existing = new StringBuilder("old");
        StringBuilder result = wire.read("value").object(existing, StringBuilder.class);

        assertSame(existing, result, "Should reuse the provided StringBuilder instance");
        assertEquals("hello", result.toString());

        bytes.releaseLast();
    }
}
