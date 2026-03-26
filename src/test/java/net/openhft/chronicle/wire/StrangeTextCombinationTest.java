/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

// A parameterized test class that tests various string serialization behaviors for different WireTypes.
class StrangeTextCombinationTest extends net.openhft.chronicle.wire.WireTestCommon {
    private WireType wireType;
    private Bytes<?> bytes;

    // Parameterized test data. Each WireType will be tested.
    public static Collection<Object[]> data() {
        Object[][] list = {
                {WireType.BINARY},
                {WireType.RAW},
                {WireType.TEXT},
                {WireType.JSON}
        };
        return Arrays.asList(list);
    }

    // Tests that a string with a leading space is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testPrependedSpace(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String prependedSpace = " hello world";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(prependedSpace);

        assertEquals(prependedSpace, wire.read().text());

    }

    // Tests that a string with a trailing space is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testPostpendedSpace(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String postpendedSpace = "hello world ";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(postpendedSpace);

        assertEquals(postpendedSpace, wire.read().text());
    }

    // Tests that a string with escape characters is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testSlashQuoteTest(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "\\\" ";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that a string with specific YAML syntax is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testYaml(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "!String{chars:hello world}";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Test class to ensure various string values are correctly serialized and deserialized using
    // Chronicle-Wire. The class contains multiple test cases, each focused on a specific string value
    // or format.

    // Tests that a string "!String" is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testString(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "!String";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that a string "!binary" is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testBinary(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "!binary";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that a string " !binary" with a leading space is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testBinaryWithSpace(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = " !binary";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that an empty string is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testEmpty(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that a null string value is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testNull(WireType wireType) {
        this.wireType = wireType;
        @Nullable final String expected = null;
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that a string with a newline character is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testNewLine(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "\n";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that a string with a Unicode null character is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testUnicode(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "\u0000";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Tests that an XML formatted string is serialized and deserialized correctly.
    @ParameterizedTest
    @MethodSource("data")
    void testXML(WireType wireType) {
        this.wireType = wireType;
        @NotNull final String expected = "<name>rob austin</name>";
        @NotNull final Wire wire = wireFactory();
        wire.write().text(expected);
        assertEquals(expected, wire.read().text());
    }

    // Helper method to create a new Wire instance using the given WireType.
    // The Wire is backed by an on-heap elastic byte buffer.
    @NotNull
    private Wire wireFactory() {
        bytes = Bytes.allocateElasticOnHeap(64);
        return wireType.apply(bytes);
    }
}
