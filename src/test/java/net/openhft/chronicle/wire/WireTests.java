/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings("rawtypes")
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
class WireTests {

    // Member variables for parameterized tests
    private WireType wireType;
    private boolean usePadding;

    // Constructor to initialize test parameters
    void initWireTests(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
    }

    // Define the parameters for the test suite
    @NotNull
    public static Collection<Object[]> data() {
        Object[][] list = {
                {WireType.BINARY, true},
                {WireType.BINARY, false},
                {WireType.TEXT, false},
                {WireType.YAML_ONLY, false},
                {WireType.JSON, false}
        };
        return Arrays.asList(list);
    }

    // Test to verify that hex representations of negative long values are handled correctly
    @DisplayName("Hex long values round-trip with negative numbers")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testHexLongNegativeTest(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final long expectedLong1 = -1;
        final long expectedLong2 = Long.MIN_VALUE;
        try {
            final Wire wire = createWire(b);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("w")
                        .int64_0x(expectedLong1);
                dc.wire().write("x")
                        .int64_0x(expectedLong2);
                dc.wire().write("y").typeLiteral(String.class);
            }

            try (DocumentContext dc = wire.readingDocument()) {
                long w = dc.wire().read("w").int64();
                assertEquals(expectedLong1, w,
                        "Hex long w should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");
                long x = dc.wire().read("x").int64();
                assertEquals(expectedLong2, x,
                        "Hex long x should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");
                Class<Object> y = dc.wire().read("y").typeLiteral();
                assertEquals(String.class, y,
                        "Type literal y should be String.class (wireType=" + wireType + ", usePadding=" + usePadding + ")");
            }
        } finally {
            b.releaseLast();
        }
    }

    // Test to verify that non-existent type literals are handled leniently
    @DisplayName("Lenient type literal returns raw type name")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testLenientTypeLiteral(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = createWire(b);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("w").typeLiteral("DoesntExist");
            }

            try (DocumentContext dc = wire.readingDocument()) {
                Type t = dc.wire().read("w").lenientTypeLiteral();
                assertEquals("DoesntExist", t.getTypeName(),
                        "Lenient type literal should return raw name (wireType=" + wireType
                                + ", usePadding=" + usePadding + ")");
            }
        } finally {
            b.releaseLast();
        }
    }

    // Test to verify that Date objects are correctly written and read
    @DisplayName("Date values round trip through wire types")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testDate(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);

        Date expected = new Date(1234567890000L);
        wire.getValueOut().object(expected);
        assertEquals(expected, wire.getValueIn().object(Date.class),
                "Date should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        /* Not sure why this would work
        final Date expectedDate = new Date(1234567890000L);
        String longDateInDefaultLocale = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy").format(expectedDate);
        wire.getValueOut().object(longDateInDefaultLocale);

        assertEquals(expectedDate, wire.getValueIn()
                .object(Date.class));

        wire.getValueOut().object("2009-02-13 23:31:30.000");

        assertEquals(new Date(1234567890000L), wire.getValueIn()
                .object(Date.class));

         */
    }

    // Test to verify that LocalDateTime objects are correctly written and read
    @DisplayName("LocalDateTime values round trip through wire types")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testLocalDateTime(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = createWire(b);
            LocalDateTime expected = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
            wire.getValueOut().object(expected);
            // Is a hint needed? Type hint varies based on the WireType
            Class<?> type = wireType == WireType.JSON ? LocalDateTime.class : Object.class;
            assertEquals(expected, wire.getValueIn().object(type),
                    "LocalDateTime should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");
        } finally {
            b.releaseLast();
        }
    }

    // Test to verify that ZonedDateTime objects are correctly written and read
    @DisplayName("ZonedDateTime values round trip through wire types")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testZonedDateTime(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        wire.getValueOut().object(expected);
        // Is a hint needed? Type hint varies based on the WireType
        Class<?> type = wireType == WireType.JSON ? ZonedDateTime.class : Object.class;
        assertEquals(expected, wire.getValueIn().object(type),
                "ZonedDateTime should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        b.releaseLast();
    }

    // Test to verify skipping values while reading both numbers and text
    @DisplayName("Skip values with numbers and strings")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testSkipValueWithNumbersAndStrings(WireType wireType, boolean usePadding) {

        initWireTests(wireType, usePadding);

        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);

        wire.write("value1").text("text");
        wire.write("number").int64(125);

        StringBuilder field;

        field = new StringBuilder();
        wire.read(field).skipValue();  // Skip the value of "value1"
        assertEquals("value1", field.toString(),
                "First field name should be value1 (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        field = new StringBuilder();
        wire.read(field).skipValue();  // Skip the value of "number"
        assertEquals("number", field.toString(),
                "Second field name should be number (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        b.releaseLast();
    }

    // Test to verify that null values are correctly written and read
    @DisplayName("Null values round-trip across wire types")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testWriteNull(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);
        wire.write().object(null);  // Write null values
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        // Read the null values back and assert
        @Nullable Object o = wire.read().object(Object.class);
        Assertions.assertNull(o, "Object null should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");
        @Nullable String s = wire.read().object(String.class);
        Assertions.assertNull(s, "String null should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");
        @Nullable RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        Assertions.assertNull(rp,
                "RetentionPolicy null should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");
        @Nullable Circle c = wire.read().object(Circle.class);  // this fails without the check.
        Assertions.assertNull(c, "Circle null should round-trip (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        b.releaseLast();
    }

    // Test to verify that a ClassHolder object with Class type is correctly marshalled and unmarshalled
    @DisplayName("Class typed marshallable round trips through wire")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testClassTypedMarshallableObject(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        assumeFalse(wireType == WireType.JSON,
                "JSON wire does not support class typed marshallable");

        @NotNull ClassHolder testClass = new ClassHolder(Boolean.class);

        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);
        wire.write().typedMarshallable(testClass);

        @Nullable ClassHolder o = wire.read().typedMarshallable();
        assertEquals(Boolean.class, o.clazz(),
                "ClassHolder should preserve clazz (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        b.releaseLast();
    }

    // Test to verify that unknown fields are cleared between read contexts
    @DisplayName("Unknown fields cleared between read contexts")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void unknownFieldsAreClearedBetweenReadContexts(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);

        // Writing "first" and "second" fields to the document
        try (final DocumentContext documentContext = wire.writingDocument()) {
            documentContext.wire().write("first").text("firstValue");
        }
        try (final DocumentContext documentContext = wire.writingDocument()) {
            documentContext.wire().write("second").text("secondValue");
        }

        // Reading from the document, asserting that unknown ("not_there") and previous ("first") fields are null
        try (final DocumentContext documentContext = wire.readingDocument()) {
            assertNull(documentContext.wire().read("not_there").text(),
                    "Unknown field should be null in first document (wireType=" + wireType
                            + ", usePadding=" + usePadding + ")");
        }
        try (final DocumentContext documentContext = wire.readingDocument()) {
            assertNull(documentContext.wire().read("first").text(),
                    "Previous field should be null in next document (wireType=" + wireType
                            + ", usePadding=" + usePadding + ")");
        }
    }

    // Test to verify peeking at YAML in the reading context, specific to BINARY wire type and padding
    @DisplayName("Reading peek YAML reflects document boundaries")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    void testReadingPeekYaml(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        assumeTrue(usePadding,
                "Padding must be enabled for peek YAML test");
        assumeTrue(wireType == WireType.BINARY,
                "Peek YAML test requires binary wire type");

        Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);

        // Asserting that the peek YAML is initially empty
        assertEquals("", wire.readingPeekYaml(),
                "Peek YAML should be empty before writes (wireType=" + wireType + ", usePadding=" + usePadding + ")");
        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                assertEquals("", wire.readingPeekYaml(),
                        "Peek YAML should stay empty during first write (wireType=" + wireType
                                + ", usePadding=" + usePadding + ")");
            });
        }

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-new").marshallable(m -> {
                m.write("some-other--new-data").int64(0);
                assertEquals("", wire.readingPeekYaml(),
                        "Peek YAML should stay empty during second write (wireType=" + wireType
                                + ", usePadding=" + usePadding + ")");
            });
        }
        assertEquals("", wire.readingPeekYaml(),
                "Peek YAML should remain empty before read (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertEquals("--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml(),
                    "Peek YAML should show first document (wireType=" + wireType + ", usePadding=" + usePadding + ")");
            dc.wire().read("some-data");
            assertEquals("--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml(),
                    "Peek YAML should remain after first read (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        }
        assertEquals("", wire.readingPeekYaml(),
                "Peek YAML should be empty after first read (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                assertEquals("", wire.readingPeekYaml(),
                        "Peek YAML should stay empty during third write (wireType=" + wireType
                                + ", usePadding=" + usePadding + ")");
            });
        }

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            int position = usePadding ? 40 : 37;
            assertEquals("# position: " + position + ", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml(),
                    "Peek YAML should show second document (wireType=" + wireType + ", usePadding=" + usePadding + ")");
            dc.wire().read("some-data");
            assertEquals("# position: " + position + ", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml(),
                    "Peek YAML should remain after second read (wireType=" + wireType + ", usePadding=" + usePadding + ")");

        }

        b.releaseLast();
    }

    @DisplayName("Wire isPresent returns true for stored field value")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    // Test to ensure that isPresent() returns true when the value is actually present
    void isPresentReturnsTrueWhenValueIsPresent(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        Bytes<?> b = Bytes.allocateElasticOnHeap();  // Create an elastic byte buffer
        final Wire wire = createWire(b);         // Create a Wire object
        wire.write("value").int32(12345);        // Write an integer value to the wire with the key "value"
        assertTrue(wire.read("value").isPresent(),
                "Value should be present (wireType=" + wireType + ", usePadding=" + usePadding + ")");
    }

    @DisplayName("Wire isPresent returns false for missing field value")
    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} padding: {1}")
    // Test to ensure that isPresent() returns false when the value is not present
    void isPresentReturnsFalseWhenValueIsNotPresent(WireType wireType, boolean usePadding) {
        initWireTests(wireType, usePadding);
        Bytes<?> b = Bytes.allocateElasticOnHeap();  // Create an elastic byte buffer
        final Wire wire = createWire(b);         // Create a Wire object
        wire.write("value").int32(12345);        // Write an integer value to the wire with the key "value"
        assertFalse(wire.read("anotherValue").isPresent(),
                "Value should be absent (wireType=" + wireType + ", usePadding=" + usePadding + ")");
    }

    // Helper method to create a Wire object
    private Wire createWire(Bytes<?> b) {
        final Wire wire = wireType.apply(b);  // Apply the wire type to the byte buffer
        wire.usePadding(usePadding);          // Set the padding option
        return wire;                          // Return the configured Wire object
    }

    // Inner class to represent a test object with a Class field

    static class ClassHolder extends SelfDescribingMarshallable {
        final Class<?> o;

        ClassHolder(Class<?> o) {
            this.o = o;
        }

        Class<?> clazz() {
            return o;
        }
    }

    // Inner class to represent a Circle, implements Marshallable for serialization
    private static class Circle implements Marshallable {
    }

}
