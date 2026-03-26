/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.ObjIntConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

// Use Parameterized runner for performing tests with different WireType instances.
class TextBinaryWireTest extends WireTestCommon {

    // The specific WireType for the current test run.
    private WireType wireType;

    // Provide the combinations of WireType instances for the tests.
    public static Collection<Object[]> combinations() {
        Object[][] list = {
                {WireType.BINARY},
                {WireType.FIELDLESS_BINARY},
                {WireType.RAW},
                {WireType.TEXT},
                {WireType.YAML},
                {WireType.JSON}
        };
        return Arrays.asList(list);
    }

    // Test the WireType's valueOf() method.
    @ParameterizedTest
    @MethodSource("combinations")
    void testValueOf(WireType wireType) {
        this.wireType = wireType;
        Wire wire = createWire();
        @NotNull WireType wt = WireType.valueOf(wire);
        assertEquals(wireType, wt);
        wire.bytes().releaseLast();

    }

    // Create a Wire instance based on the wireType of the test.
    private Wire createWire() {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        return wire;
    }

    // Test reading a document location from a Wire instance.
    @ParameterizedTest
    @MethodSource("combinations")
    void readingDocumentLocation(WireType wireType) {
        this.wireType = wireType;
        Wire wire = createWire();
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        wire.writeDocument(true, w -> w.write("header").text("data"));
        long position = wire.bytes().writePosition();
        wire.writeDocument(false, w -> w.write("message").text("text"));

        try (DocumentContext dc = wire.readingDocument(position)) {
            assertEquals("text", dc.wire().read(() -> "message").text());
        }
        wire.bytes().releaseLast();
    }

    // Test reading comments from a Wire instance.
    @ParameterizedTest
    @MethodSource("combinations")
    void testReadComment(WireType wireType) {
        this.wireType = wireType;
        // Only execute for specific wireTypes.
        assumeTrue(wireType == WireType.TEXT || wireType == WireType.BINARY || wireType == WireType.YAML);

        Wire wire = createWire();
        wire.writeComment("This is a comment");
        @NotNull StringBuilder sb = new StringBuilder();
        wire.readComment(sb);
        assertEquals("This is a comment", sb.toString());

        wire.bytes().releaseLast();
    }

    // Test reading fields as objects from a Wire instance.
    @ParameterizedTest
    @MethodSource("combinations")
    void readFieldAsObject(WireType wireType) {
        this.wireType = wireType;
        // Exclude certain wireTypes.
        assumeFalse(wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY);

        Wire wire = createWire();
        wire.write("CLASS").text("class")
                .write("RUNTIME").text("runtime");
        assertEquals(RetentionPolicy.CLASS, wire.readEvent(RetentionPolicy.class));
        assertEquals("class", wire.getValueIn().text());
        assertEquals(RetentionPolicy.RUNTIME, wire.readEvent(RetentionPolicy.class));
        assertEquals("runtime", wire.getValueIn().text());

        assertNull(wire.readEvent(RetentionPolicy.class));

        wire.bytes().releaseLast();
    }

    // Test reading fields as long values from a Wire instance.
    @ParameterizedTest
    @MethodSource("combinations")
    void readFieldAsLong(WireType wireType) {
        this.wireType = wireType;
        // Exclude certain wireTypes.
        assumeFalse(wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY);

        Wire wire = createWire();
        // todo fix to ensure a field number is used.
        wire.writeEvent(Long.class, 1L).text("class")
                .writeEvent(Long.class, 2L).text("runtime");

        assertEquals((Long) 1L, wire.readEvent(Long.class));
        assertEquals("class", wire.getValueIn().text());
        @NotNull StringBuilder sb = new StringBuilder();
        wire.readEventName(sb);
        assertEquals("2", sb.toString());
        assertEquals("runtime", wire.getValueIn().text());

        assertNull(wire.readEvent(RetentionPolicy.class));

        wire.bytes().releaseLast();
    }

    // Test conversion of different values to numeric values in a Wire instance.
    @ParameterizedTest
    @MethodSource("combinations")
    void testConvertToNum(WireType wireType) {
        this.wireType = wireType;
        // Exclude certain wireTypes.
        assumeFalse(wireType == WireType.RAW || /* No support for bool conversions */ wireType == WireType.YAML);

        Wire wire = createWire();
        wire.write("a").bool(false)
                .write("b").bool(true)
                .write("c").float32(2.0f)
                .write("d").float64(3.0);

        @NotNull final ObjIntConsumer<Integer> assertEquals = (expected, actual) -> assertEquals((long) expected, actual);
        wire.read(() -> "a").int32(0, assertEquals);
        wire.read(() -> "b").int32(1, assertEquals);
        wire.read(() -> "c").int32(2, assertEquals);
        wire.read(() -> "d").int32(3, assertEquals);

        wire.bytes().releaseLast();
    }
}
