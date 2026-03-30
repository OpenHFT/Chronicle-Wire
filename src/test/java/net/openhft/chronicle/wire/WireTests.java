/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
import static org.junit.jupiter.api.Assumptions.*;

@SuppressWarnings("rawtypes")
class WireTests {

    // Member variables for parameterized tests
    private WireType wireType;
    private boolean usePadding;

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
    @ParameterizedTest
    @MethodSource("data")
    void testHexLongNegativeTest(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
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
                assertEquals(expectedLong1, w);
                long x = dc.wire().read("x").int64();
                assertEquals(expectedLong2, x);
                Class<Object> y = dc.wire().read("y").typeLiteral();
                assertSame(String.class, y);
            }
        } finally {
            b.releaseLast();
        }
    }

    // Test to verify that non-existent type literals are handled leniently
    @ParameterizedTest
    @MethodSource("data")
    void testLenientTypeLiteral(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = createWire(b);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("w").typeLiteral("DoesntExist");
            }

            try (DocumentContext dc = wire.readingDocument()) {
                Type t = dc.wire().read("w").lenientTypeLiteral();
                assertEquals("DoesntExist", t.getTypeName());
            }
        } finally {
            b.releaseLast();
        }
    }

    // Test to verify that Date objects are correctly written and read
    @ParameterizedTest
    @MethodSource("data")
    void testDate(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);

        wire.getValueOut()
                .object(new Date(1234567890000L));
        assertEquals(new Date(1234567890000L), wire.getValueIn()
                .object(Date.class));

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
    @ParameterizedTest
    @MethodSource("data")
    void testLocalDateTime(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            final Wire wire = createWire(b);
            LocalDateTime expected = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
            wire.getValueOut().object(expected);
            // Is a hint needed? Type hint varies based on the WireType
            Class<?> type = wireType == WireType.JSON ? LocalDateTime.class : Object.class;
            assertEquals(expected, wire.getValueIn().object(type));
        } finally {
            b.releaseLast();
        }
    }

    // Test to verify that ZonedDateTime objects are correctly written and read
    @ParameterizedTest
    @MethodSource("data")
    void testZonedDateTime(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        wire.getValueOut().object(expected);
        // Is a hint needed? Type hint varies based on the WireType
        Class<?> type = wireType == WireType.JSON ? ZonedDateTime.class : Object.class;
        assertEquals(expected, wire.getValueIn().object(type));

        b.releaseLast();
    }

    // Test to verify skipping values while reading both numbers and text
    @ParameterizedTest
    @MethodSource("data")
    void testSkipValueWithNumbersAndStrings(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;

        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);

        wire.write("value1").text("text");
        wire.write("number").int64(125);

        StringBuilder field;

        field = new StringBuilder();
        wire.read(field).skipValue();  // Skip the value of "value1"
        assertEquals("value1", field.toString());

        field = new StringBuilder();
        wire.read(field).skipValue();  // Skip the value of "number"
        assertEquals("number", field.toString());

        b.releaseLast();
    }

    // Test to verify that null values are correctly written and read
    @ParameterizedTest
    @MethodSource("data")
    void testWriteNull(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);
        wire.write().object(null);  // Write null values
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        // Read the null values back and assert
        @Nullable Object o = wire.read().object(Object.class);
        assertNull(o);
        @Nullable String s = wire.read().object(String.class);
        assertNull(s);
        @Nullable RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        assertNull(rp);
        @Nullable Circle c = wire.read().object(Circle.class);  // this fails without the check.
        assertNull(c);

        b.releaseLast();
    }

    // Test to verify that a TestClass object with Class type is correctly marshalled and unmarshalled
    @ParameterizedTest
    @MethodSource("data")
    void testClassTypedMarshallableObject(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        assumeFalse(wireType == WireType.JSON);

        @NotNull TestClass testClass = new TestClass(Boolean.class);

        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);
        wire.write().typedMarshallable(testClass);

        @Nullable TestClass o = wire.read().typedMarshallable();
        assertSame(Boolean.class, o.clazz());

        b.releaseLast();
    }

    // Test to verify that unknown fields are cleared between read contexts
    @ParameterizedTest
    @MethodSource("data")
    void unknownFieldsAreClearedBetweenReadContexts(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
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
            assertNull(documentContext.wire().read("not_there").text());
        }
        try (final DocumentContext documentContext = wire.readingDocument()) {
            assertNull(documentContext.wire().read("first").text());
        }
    }

    // Test to verify peeking at YAML in the reading context, specific to BINARY wire type and padding
    @ParameterizedTest
    @MethodSource("data")
    void testReadingPeekYaml(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        assumeTrue(usePadding);
        assumeTrue(wireType == WireType.BINARY);

        Bytes<?> b = Bytes.allocateElasticOnHeap();
        final Wire wire = createWire(b);

        // Asserting that the peek YAML is initially empty
        assertEquals("", wire.readingPeekYaml());
        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                assertEquals("", wire.readingPeekYaml());
            });
        }

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-new").marshallable(m -> {
                m.write("some-other--new-data").int64(0);
                assertEquals("", wire.readingPeekYaml());
            });
        }
        assertEquals("", wire.readingPeekYaml());

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertEquals("" +
                    "--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            assertEquals("" +
                    "--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }
        assertEquals("", wire.readingPeekYaml());

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                assertEquals("", wire.readingPeekYaml());
            });
        }

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            int position = usePadding ? 40 : 37;
            assertEquals("" +
                    "# position: " + position + ", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            assertEquals("" +
                    "# position: " + position + ", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }

        b.releaseLast();
    }

    // Test to ensure that isPresent() returns true when the value is actually present
    @ParameterizedTest
    @MethodSource("data")
    void isPresentReturnsTrueWhenValueIsPresent(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        Bytes<?> b = Bytes.allocateElasticOnHeap();  // Create an elastic byte buffer
        final Wire wire = createWire(b);         // Create a Wire object
        wire.write("value").int32(12345);        // Write an integer value to the wire with the key "value"
        assertTrue(wire.read("value").isPresent()); // Assert that reading the key "value" from the wire is present
    }

    // Test to ensure that isPresent() returns false when the value is not present
    @ParameterizedTest
    @MethodSource("data")
    void isPresentReturnsFalseWhenValueIsNotPresent(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
        Bytes<?> b = Bytes.allocateElasticOnHeap();  // Create an elastic byte buffer
        final Wire wire = createWire(b);         // Create a Wire object
        wire.write("value").int32(12345);        // Write an integer value to the wire with the key "value"
        assertFalse(wire.read("anotherValue").isPresent());  // Assert that reading a non-existing key from the wire is not present
    }

    // Helper method to create a Wire object
    private Wire createWire(Bytes<?> b) {
        Wire wire = wireType.apply(b);  // Apply the wire type to the byte buffer
        wire.usePadding(usePadding);          // Set the padding option
        return wire;                          // Return the configured Wire object
    }

    // Inner class to represent a test object with a Class field
    static class TestClass extends SelfDescribingMarshallable {
        Class<?> o;

        TestClass(Class<?> o) {
            this.o = o;
        }

        Class<?> clazz() {
            return o;
        }
    }

    // Inner class to represent a Circle, implements Marshallable for serialization
    private class Circle implements Marshallable {
    }
}
