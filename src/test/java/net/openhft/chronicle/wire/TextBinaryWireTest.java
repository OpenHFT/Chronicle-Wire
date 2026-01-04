/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Use Parameterized runner for performing tests with different WireType instances.
@SuppressWarnings({"deprecation", "removal"})
class TextBinaryWireTest extends WireTestCommon {

    // The specific WireType for the current test run.
    private WireType wireType;

    // Constructor to initialize the test with a specific WireType instance.
    void initTextBinaryWireTest(WireType wireType) {

        this.wireType = wireType;
    }

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
    @DisplayName("Identifies wire type from runtime wire instance lookup")
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    void testValueOf(WireType wireType) {
        initTextBinaryWireTest(wireType);
        Wire wire = createWire();
        @NotNull WireType wt = WireType.valueOf(wire);
        assertEquals(wireType, wt, "wire type should be correctly identified from wire instance");
        wire.bytes().releaseLast();

    }

    // Create a Wire instance based on the wireType of the test.
    private Wire createWire() {
        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        return wire;
    }

    // Test reading a document location from a Wire instance.
    @DisplayName("Reads document at explicit position for each wire type")
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    void readingDocumentLocation(WireType wireType) {
        initTextBinaryWireTest(wireType);
        Wire wire = createWire();
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        wire.writeDocument(true, w -> w.write("header").text("data"));
        long position = wire.bytes().writePosition();
        wire.writeDocument(false, w -> w.write("message").text("text"));

        try (DocumentContext dc = wire.readingDocument(position)) {
            assertEquals("text", dc.wire().read(() -> "message").text(), "document should be readable at specific position regardless of wire format");
        }
        wire.bytes().releaseLast();
    }

    // Test reading comments from a Wire instance.
    @DisplayName("Reads comments across supported wire types")
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    void testReadComment(WireType wireType) {
        initTextBinaryWireTest(wireType);
        // Only execute for specific wireTypes.
        assumeTrue(wireType == WireType.TEXT || wireType == WireType.BINARY || wireType == WireType.YAML,
                "only text, binary, and yaml wire types support comments");

        Wire wire = createWire();
        wire.writeComment("This is a comment");
        @NotNull StringBuilder sb = new StringBuilder();
        wire.readComment(sb);
        assertEquals("This is a comment", sb.toString(), "comment should roundtrip through wire format without modification");

        wire.bytes().releaseLast();
    }

    // Test reading fields as objects from a Wire instance.
    @DisplayName("Reads enum events and values as objects")
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    void readFieldAsObject(WireType wireType) {
        initTextBinaryWireTest(wireType);
        // Exclude certain wireTypes.
        assumeFalse(wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY,
                "raw and fieldless binary do not support enum event fields, wireType=" + wireType);

        Wire wire = createWire();
        wire.write("CLASS").text("class")
                .write("RUNTIME").text("runtime");
        assertEquals(RetentionPolicy.CLASS, wire.readEvent(RetentionPolicy.class), "first enum event should be read as CLASS from wire format");
        assertEquals("class", wire.getValueIn().text(), "first enum value should match written text after readEvent");
        assertEquals(RetentionPolicy.RUNTIME, wire.readEvent(RetentionPolicy.class), "second enum event should be read as RUNTIME from wire format");
        assertEquals("runtime", wire.getValueIn().text(), "second enum value should match written text after readEvent");

        assertNull(wire.readEvent(RetentionPolicy.class), "readEvent should return null when no more events available in wire");

        wire.bytes().releaseLast();
    }

    // Test reading fields as long values from a Wire instance.
    @DisplayName("Reads numeric event keys and values")
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    void readFieldAsLong(WireType wireType) {
        initTextBinaryWireTest(wireType);
        // Exclude certain wireTypes.
        assumeFalse(wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY,
                "raw and fieldless binary do not support numeric event fields, wireType=" + wireType);

        Wire wire = createWire();
        // todo fix to ensure a field number is used.
        wire.writeEvent(Long.class, 1L).text("class")
                .writeEvent(Long.class, 2L).text("runtime");

        assertEquals((Long) 1L, wire.readEvent(Long.class), "first long event key should be read as 1L from wire format");
        assertEquals("class", wire.getValueIn().text(), "first long event value should match written text");
        @NotNull StringBuilder sb = new StringBuilder();
        wire.readEventName(sb);
        assertEquals("2", sb.toString(), "second event name should be read as numeric string '2' from wire format");
        assertEquals("runtime", wire.getValueIn().text(), "second long event value should match written text");

        assertNull(wire.readEvent(RetentionPolicy.class), "readEvent should return null for different type when no more events available");

        wire.bytes().releaseLast();
    }

    // Test conversion of different values to numeric values in a Wire instance.
    @DisplayName("Converts boolean and numeric values to int32")
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    void testConvertToNum(WireType wireType) {
        initTextBinaryWireTest(wireType);
        // Exclude certain wireTypes.
        assumeFalse(wireType == WireType.RAW || /* No support for bool conversions */ wireType == WireType.YAML,
                "raw and yaml wire types do not support numeric conversions");

        Wire wire = createWire();
        wire.write("a").bool(false)
                .write("b").bool(true)
                .write("c").float32(2.0f)
                .write("d").float64(3.0);

        int[] actual = new int[4];
        wire.read(() -> "a").int32(0, (expected, value) -> actual[0] = value);
        wire.read(() -> "b").int32(1, (expected, value) -> actual[1] = value);
        wire.read(() -> "c").int32(2, (expected, value) -> actual[2] = value);
        wire.read(() -> "d").int32(3, (expected, value) -> actual[3] = value);

        Assertions.assertEquals(0, actual[0], "boolean false should convert to int32 value 0 when read from wire format");
        Assertions.assertEquals(1, actual[1], "boolean true should convert to int32 value 1 when read from wire format");
        Assertions.assertEquals(2, actual[2], "float32 value 2.0 should convert to int32 value 2 when read from wire format");
        Assertions.assertEquals(3, actual[3], "float64 value 3.0 should convert to int32 value 3 when read from wire format");

        wire.bytes().releaseLast();
    }
}
