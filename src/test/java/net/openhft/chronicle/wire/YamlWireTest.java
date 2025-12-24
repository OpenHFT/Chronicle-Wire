/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.TextWireTest.ABCD;
import net.openhft.chronicle.wire.converter.NanoTime;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.RetentionPolicy;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticDirect;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static net.openhft.chronicle.wire.TextWireTest.ABC;
import static net.openhft.chronicle.wire.TextWireTest.WithEnumSet;
import static net.openhft.chronicle.wire.YamlTokeniserTest.doTest;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"rawtypes", "unchecked", "try", "serial", "deprecation", "removal"})
public class YamlWireTest extends AbstractWireTest {
    private static final Wire wire = Wire.newYamlWireOnHeap(); // Initialize a static YAML wire
    private boolean usePadding; // Flag to indicate if padding should be used

    // Constructor for initializing the usePadding flag
    public void initYamlWireTest(boolean usePadding) {
        this.usePadding = usePadding;
    }

    // Defines the set of parameters to be used for the test
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Test case for adding a comment to a wire
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void comment(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        wire.writeComment("\thi: omg"); // Write a comment to the wire
        wire.write("hi").text("there"); // Write key-value pair to the wire
        // Assert that reading the value back works
        assertEquals("there",
                wire.read("hi")
                        .text(), "comment read should return the value written after the comment");
    }

    // Test case for reading a null value
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testTypeInsteadOfField(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = YamlWire.from("!!null \"\"");
        StringBuilder sb = new StringBuilder();
        wire.read(sb)
                .object(Object.class);
        assertEquals(0, sb.length(), "field name should be empty when reading type instead of field");
    }

    // Test case for writing and reading an object with TreeMap
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void writeObjectWithTreeMap(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire(); // Create a new Wire object
        // Initialize and populate an ObjectWithTreeMap
        ObjectWithTreeMap value = new ObjectWithTreeMap();
        value.map.put("hello", "world");
        wire.write().object(value); // Write the object to the wire


        // Deserialization tests here
        ObjectWithTreeMap value2 = new ObjectWithTreeMap();
        wire.read().object(value2, ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value2.map.toString(), "TreeMap deserialized with explicit class should preserve content");

        wire.bytes().readPosition(0);
        wire.getValueIn().resetState();
        ObjectWithTreeMap value3 = new ObjectWithTreeMap();
        wire.read().object(value3, Object.class);
        assertEquals("{hello=world}", value3.map.toString(), "TreeMap deserialized as Object should preserve content");

        wire.bytes().readPosition(0);
        wire.getValueIn().resetState();
        ObjectWithTreeMap value4 = wire.read().object(ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value4.map.toString(), "TreeMap deserialized directly should preserve content");
    }

    // Test case for hexadecimal integer values
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testFromString2(boolean usePadding) {
        initYamlWireTest(usePadding);
        // Loop over integer values and assert their deserialization
        for (int i = 0; i <= 256; i++) {
            Wire w = YamlWire.from(
                    "data: 0x" + Integer.toHexString(i).toUpperCase() + ",\n" +
                            "data2: 0x" + Integer.toHexString(i).toLowerCase());
            assertEquals(i, w.read("data").int64(), "uppercase hex value should parse correctly");
            assertEquals(i, w.read("data2").int64(), "lowercase hex value should parse correctly");
        }
    }

    // Test case for a large hexadecimal value
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testLargeHex(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire w = YamlWire.from(
                "magic: 0xCAFEBABE\n");
        assertEquals(3405691582L, w.read("magic").int64(), "large hex value 0xCAFEBABE should parse to correct long");
    }

    // Test case for C-style octal integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testCStyleOctal(boolean usePadding) {
        initYamlWireTest(usePadding);
        // Do we need it?
        Wire w = YamlWire.from("perms: 0644\n");
        assertEquals(420, w.read("perms").int64(), "C-style octal 0644 should parse to decimal 420");
    }

    // Test case for YAML-style octal integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testYamlStyleOctal(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire w = YamlWire.from("perms: 0o750\n");
        assertEquals(488, w.read("perms").int64(), "YAML-style octal 0o750 should parse to decimal 488");
    }

    // Test case for parsing a complex YAML string to an Object
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testFromString(boolean usePadding) {
        initYamlWireTest(usePadding);
        @Nullable Object w = WireType.YAML.fromString("changedRow: {\n" +
                "  row: [\n" +
                "  ],\n" +
                "  oldRow: {\n" +
                "    volume: 26880400.0,\n" +
                "    high: 108.3,\n" +
                "    adjClose: 107.7,\n" +
                "    low: 107.51,\n" +
                "    close: 107.7,\n" +
                "    key: !java.util.Date 1473116400000,\n" +
                "    open: 107.9\n" +
                "  }\n" +
                "}");
        assertInstanceOf(Map.class, w, "complex YAML nested structure should deserialize to Map");
    }

    // Test case for writing multiple empty fields
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testWrite(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        wire.write(); // Write a few empty fields
        wire.write();
        wire.write();
        // Assert the output string matches the expected empty fields
        assertEquals("\"\": \"\": \"\": ", wire.toString(), "three empty writes should produce three empty field pairs");
    }

    // Test case for writing data in binary and reading it back in text
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testWriteToBinaryAndTriesToConvertToText(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap()); // Create a binary wire
        wire.usePadding(usePadding); // Set padding

        // Create a data map
        @NotNull Map<String, String> data = Collections.singletonMap("key", "value");

        // Create a nested map
        @NotNull HashMap map = new HashMap();
        map.put("some", data);
        map.put("some-other", data);

        try (DocumentContext dc = wire.writingDocument()) {
            wire.write("map").object(map); // Write the map object
        }

        // Convert the binary data to text and read it
        final String textYaml = Wires.fromSizePrefixedBlobs(wire);
        @Nullable Object o = WireType.YAML.fromString(textYaml);
        // Assert the object's string representation is as expected
        Assertions.assertEquals("{map={some={key=value}, some-other={key=value}}}", o.toString(), "binary to text conversion should preserve nested map structure");
    }

    @NotNull
    @Override
    protected Wire createWire() {
        wire.reset(); // Reset the wire
        wire.usePadding(usePadding); // Set padding
        return wire;
    }

    // Test case for reading fields
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testRead(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object
        wire.write();  // Write an empty field
        wire.bytes().append("\n");  // Add a newline
        wire.write(BWKey.field1);  // Write a field with predefined key
        wire.bytes().append("\n");  // Add a newline
        wire.write(() -> "Test");  // Write a custom field
        wire.bytes().append("\n");  // Add a newline
        wire.read(); // Read a few fields
        wire.read();
        wire.read();
        assertEquals(0, wire.bytes().readRemaining(), "all written fields should be consumed");
        // It should be safe to read beyond the available bytes
        wire.read();
    }

    // Test case to demonstrate reading values with specific keys
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testRead1(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object
        wire.write().text("1");
        wire.write(BWKey.field1).text("2");
        wire.write(() -> "Test").text("3");

        // The following reads are considered okay because an empty or blank field matches anything
        wire.read(BWKey.field1).text();
        wire.read(BWKey.field1).text();

        // This read doesn't match the field
        wire.read(BWKey.field1).text();

        assertEquals(0, wire.bytes().readRemaining(), "all text values should be consumed after reads");

        // Verify that it's safe to read beyond the available data
        wire.read();
    }

    // Test case to demonstrate reading with dynamically built names
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testRead2(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object
        wire.write().text("");
        wire.write(BWKey.field1).text("");
        @NotNull String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(name1).text("");

        // Read into a dynamically built name, asserting the captured name at each step
        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name).text();
        assertEquals(0, name.length(), "first field should have empty name");

        wire.read(name).text();
        assertEquals(BWKey.field1.name(), name.toString(), "second field should capture field1 name");

        wire.read(name).text();
        assertEquals(name1, name.toString(), "third field should capture long field name");

        assertEquals(0, wire.bytes().readRemaining(), "all fields with dynamic names should be consumed");

        // Verify that it's safe to read beyond the available data
        assertNull(wire.read().text(), "reading beyond available data should return null");
    }

    // Test case for 8-bit integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void int8(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object

        WireSmallIntTestSupport.writeInt8Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readInt8Triplet(wire);

        assertEquals(0, wire.bytes().readRemaining(), "all int8 values should be consumed");
        wire.read();
    }

    // Test case for 16-bit integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void int16(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object

        WireSmallIntTestSupport.writeInt16Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readInt16Triplet(wire);

        assertEquals(0, wire.bytes().readRemaining(), "all int16 values should be consumed");
        wire.read();
    }

    // Test case for handling 8-bit unsigned integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void uint8(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        WireSmallIntTestSupport.writeUint8Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readUint8Triplet(wire);

        assertEquals(0, wire.bytes().readRemaining(), "all uint8 values should be consumed");
        wire.read();
    }

    // Test case for handling 16-bit unsigned integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void uint16(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        WireSmallIntTestSupport.writeUint16Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readUint16Triplet(wire);

        assertEquals(0, wire.bytes().readRemaining(), "all uint16 values should be consumed");
        wire.read();
    }

    // Test case for handling 32-bit unsigned integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void uint32(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        WireSmallIntTestSupport.writeUint32Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readUint32Triplet(wire);

        assertEquals(0, wire.bytes().readRemaining(), "all uint32 values should be consumed");
        wire.read();
    }

    // Test case for handling 32-bit signed integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void int32(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        WireSmallIntTestSupport.writeInt32Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readInt32Triplet(wire);

        assertEquals(0, wire.bytes().readRemaining(), "all int32 values should be consumed");
        wire.read();
    }

    // Test case for handling 64-bit signed integers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void int64(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object
        wire.write().int64(1); // Write a few 64-bit integers
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);

        // Validate YAML output and string representation
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString(), "int64 values should serialize as plain numbers in YAML format");

        // Read the 64-bit integers back and validate
        @NotNull AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get(), "int64 value should match expected sequence");
        });

        assertEquals(0, wire.bytes().readRemaining(), "all int64 values should be consumed");
        wire.read();  // Test that reading beyond available data is safe
    }

    // Test case for handling 64-bit floating-point numbers
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void float64(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object
        wire.write().float64(1);  // Write a few 64-bit floats
        wire.write(BWKey.field1).float64(2);
        wire.write(() -> "Test").float64(3);
        assertEquals("\"\": 1.0\n" +
                "field1: 2.0\n" +
                "Test: 3.0\n", wire.toString(), "float64 values should serialize with decimal point in YAML format");

        // Validate the output and read it back for comparison
        expectWithSnakeYaml("{=1.0, field1=2.0, Test=3.0}", wire);

        // Custom object to hold floating-point value for verification
        class Floater {
            double f;

            void set(double d) {
                f = d;
            }
        }
        @NotNull Floater n = new Floater();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n, Floater::set);
            assertEquals(e, n.f, 0.0, "float64 value should match expected sequence");
        });

        assertEquals(0, wire.bytes().readRemaining(), "all float64 values should be consumed");
        wire.read();  // Test that reading beyond available data is safe
    }

    // Test case for handling text data types
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void text(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object
        wire.write().text("Hello");  // Write a type text
        wire.write(BWKey.field1).text("world");  // Write another type text
        @NotNull String name = "Long field name which is more than 32 characters, \\ \nBye";

        wire.write(() -> "Test")
                .text(name);

        // Validate the output and read it back for comparison
        expectWithSnakeYaml("{=Hello, field1=world, Test=Long field name which is more than 32 characters, \\ \n" +
                "Bye}", wire);
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, \\\\ \\nBye\"\n", wire.toString(), "text values should escape special characters and quote long strings");

        // Read the text back and validate
        @NotNull StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name).forEach(e -> {
            assertNotNull(wire.read().textTo(sb), "text read should not return null");
            assertEquals(e, sb.toString(), "text value should match expected string");
        });

        assertEquals(0, wire.bytes().readRemaining(), "all text values should be consumed");
        wire.read();  // Test that reading beyond available data is safe
    }

    // Test case for handling type prefix data types
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void type(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();  // Create a new Wire object
        wire.write().typePrefix("MyType").text("");  // Write a type prefix
        wire.write(BWKey.field1).typePrefix("AlsoMyType").text("");  // Write another type prefix
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write(() -> "Test").typePrefix(name1).text("");  // Write a long type prefix
        wire.writeComment("");
        // TODO fix how types are serialized.
        // expectWithSnakeYaml(wire, "{=1, field1=2, Test=3}");
        assertEquals("\"\": !MyType \"\"\n" +
                "field1: !AlsoMyType \"\"\n" +
                "Test: !" + name1 + " \"\"\n" +
                "# \n", wire.toString(), "type prefixes should serialize with YAML type tags");

        // Read the type prefix back and validate
        Stream.of("MyType", "AlsoMyType", name1).forEach(e ->
            wire.read()
                    .typePrefix(e, Assertions::assertEquals)
                    .text());

        assertEquals(0, wire.bytes().readRemaining(), 1, "all type-prefixed values should be consumed");
        wire.read();  // Test that reading beyond available data is safe
    }

    // Test case for boolean types
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testBool(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        WirePrimitiveTestSupport.assertBooleanRoundTrip(wire);
    }

    // Test case for 32-bit floating point types
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testFloat32(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        WirePrimitiveTestSupport.assertFloat32RoundTrip(wire, this);
    }

    // Test case for time representation
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testTime(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire(); // Create a new Wire object
        LocalTime now = LocalTime.now(); // Get current time

        WirePrimitiveTestSupport.writeTimes(wire, now);
        assertEquals(WirePrimitiveTestSupport.expectedTimeString(now), wire.toString(), "LocalTime should serialize in expected format");
        WirePrimitiveTestSupport.assertTimes(wire, now);
    }


    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testDate(boolean usePadding) {
        initYamlWireTest(usePadding);
        final Wire wire = createWire();
        WireTemporalTestSupport.assertLocalDates(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testUuid(boolean usePadding) {
        initYamlWireTest(usePadding);
        final Wire wire = createWire();
        WireTemporalTestSupport.assertUuids(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testTypeWithoutSpace(boolean usePadding) {
        initYamlWireTest(usePadding);
        final Wire wire = createWire();

        WireTestSupport.assertTypeWithoutSpace(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testNANValue(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();

        WireTestSupport.assertNanValues(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testQuoting(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();
        wire.bytes().append(
                "nonesingle: \\\n" +
                        "nonedouble: \\\\\n" +
                        "singleself: ''''\n" +
                        "singleselfself: ''''''\n" +
                        "singlesingle: '\\'\n" +
                        "singledouble: '\\\\'\n" +
                        "doubleself: \"\\\"\"\n" +
                        "doublesingle: \"\\\\\"\n" +
                        "doubledouble: \"\\\\\\\\\"\n");
        assertEquals("\\", wire.read("nonesingle").readString(), "unquoted single backslash should parse correctly");
        assertEquals("\\\\", wire.read("nonedouble").readString(), "unquoted double backslash should parse correctly");
        assertEquals("'", wire.read("singleself").readString(), "single-quoted single quote should parse correctly");
        assertEquals("''", wire.read("singleselfself").readString(), "single-quoted double quote should parse correctly");
        assertEquals("\\", wire.read("singlesingle").readString(), "single-quoted single backslash should parse correctly");
        assertEquals("\\\\", wire.read("singledouble").readString(), "single-quoted double backslash should parse correctly");
        assertEquals("\"", wire.read("doubleself").readString(), "double-quoted double quote should parse correctly");
        assertEquals("\\", wire.read("doublesingle").readString(), "double-quoted single backslash should parse correctly");
        assertEquals("\\\\", wire.read("doubledouble").readString(), "double-quoted double backslash should parse correctly");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testBinary(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();
        wire.bytes().append("b: !byte[] !!binary AAAAAAA=\n" +
                "c: !!binary CCCCCCCC\n");
        byte[] b = (byte[]) wire.read("b").object();
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, b, "binary data should decode from base64 to byte array: " + Arrays.toString(b));
        assertEquals(BytesStore.wrap(new byte[]{8, ' ', -126, 8, ' ', -126}), wire.read("c").object(), "binary data without type should decode to BytesStore");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testABCDBytes(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        @NotNull Wire wire = createWire();

        WireAbcTestSupport.assertAbcdBytes(wire, true);
    }

    // Test the string building behavior for ABC objects with Wire.
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testABCStringBuilder(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        @NotNull Wire wire = createWire();

        WireAbcTestSupport.assertAbcStringBuilder(wire, Arrays.asList("This is an A", "This is a B", "And that's a C"));
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testBytes(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();
        @NotNull byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;
        WireBytesTestSupport.exerciseBytesRoundTrip(wire, WireBytesTestSupport.helloBytes(), WireBytesTestSupport.quoteBytes(), allBytes);
        @NotNull Bytes<?> allBytes2 = allocateElasticOnHeap();
        WireBytesTestSupport.assertBytesRoundTrip(wire, allBytes, allBytes2);
    }


    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testContextDump(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> from = Bytes.from("# comment\n" +
                "A: \n" +
                "  b: 1234\n" +
                "  c: hi\n" +
                "  d: abc\n" +
                "B: \n" +
                "  c: lo\n" +
                "  d: xyz\n" +
                "C: see\n");
        try {
            YamlWire yw = new YamlWire(from);
            assertEquals("[\n" +
                    "  { token: STREAM_START, indent: -1, keys: !!null \"\" }\n" +
                    "]\n", yw.dumpContext(), "initial context dump should show STREAM_START token");
            yw.read("C")
                    .text();
            assertEquals("[\n" +
                    "  { token: STREAM_START, indent: -1, keys: !!null \"\" },\n" +
                    "  { token: DIRECTIVES_END, indent: -1, keys: !!null \"\" },\n" +
                    "  { token: MAPPING_START, indent: 0, keys: !net.openhft.chronicle.wire.YamlKeys { count: 2, offsets: [ 10, 41, 0, 0, 0, 0, 0 ] } }\n" +
                    "]\n", yw.dumpContext(), "context dump after read should show parsing progress");
            assertEquals("{c=lo, d=xyz}", "" + yw.read("B").object(), "out-of-order read B should return correct map");
            assertEquals("{b=1234, c=hi, d=abc}", "" + yw.read("A").object(), "out-of-order read A should return correct map");

        } finally {
            from.releaseLast();
        }
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testContextDump2(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> from = Bytes.from("#\nb: AA\nc: {}\nd: \n  A: 1\n  B: 2\ne: end");
        try {
            YamlWire yw = new YamlWire(from);
            yw.read("a").text();
            assertEquals("[\n" +
                    "  { token: STREAM_START, indent: -1, keys: !!null \"\" },\n" +
                    "  { token: DIRECTIVES_END, indent: -1, keys: !!null \"\" },\n" +
                    "  { token: MAPPING_START, indent: 0, keys: !net.openhft.chronicle.wire.YamlKeys { count: 4, offsets: [ 2, 8, 14, 32, 0, 0, 0 ] } }\n" +
                    "]\n", yw.dumpContext(), "context dump should show key offsets for all fields");
            assertEquals("AA", "" + yw.read("b").object(), "field b should read as text AA");
            assertEquals("{}", "" + yw.read("c").object(), "field c should read as empty map");
            assertEquals("{A=1, B=2}", "" + yw.read("d").object(), "field d should read as nested map");
            assertEquals("end", "" + yw.read("e").object(), "field e should read as text end");

        } finally {
            from.releaseLast();
        }
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testConsumeAny(boolean usePadding) {
        initYamlWireTest(usePadding);
        Bytes<?> from = Bytes.from("A: \n" +
                "  b: 1234\n" +
                "  c: hi\n" +
                "  d: abc\n" +
                "B: \n" +
                "  c: lo\n" +
                "  d: xyz\n" +
                "C: see\n");
        try {
            YamlWire yw = new YamlWire(from);
            yw.startEvent();
            yw.read().skipValue();
            assertEquals("B", yw.readEvent(String.class), "readEvent should return field name B");
            yw.getValueIn().skipValue();
            assertEquals("C", yw.readEvent(String.class), "readEvent should return field name C");
            yw.endEvent();
        } finally {
            from.releaseLast();
        }
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testMapReadAndWriteStrings(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = new YamlWire(bytes);

        @NotNull final Map<String, String> expected = new LinkedHashMap<>();

        expected.put("hello", "world");
        expected.put("hello1", "world1");
        expected.put("hello2", "world2");

        wire.writeDocument(false, o ->
            o.writeEventName(() -> "example")
                    .map(expected));

        assertEquals("--- !!data\n" +
                        "example: {\n" +
                        "  hello: world,\n" +
                        "  hello1: world1,\n" +
                        "  hello2: world2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes), "string map should serialize to YAML with proper formatting");
        @NotNull final Map<String, String> actual = new LinkedHashMap<>();
        wire.readDocument(null, c -> c.read(() -> "example").marshallableAsMap(String.class, String.class, actual));
        assertEquals(expected, actual, "deserialized map should match original map");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testMapInMap(boolean usePadding) {
        initYamlWireTest(usePadding);
        WireMapTestSupport.assertMapInMap("WithMap: {\n" +
                "  innerMap: {\n" +
                "    AUDUSD: AUDUSD1,\n" +
                "    USDPLN: USDPLN1\n" +
                "  },\n" +
                "}");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testMapInMapWithQuestionMarks(boolean usePadding) {
        initYamlWireTest(usePadding);
        WireMapTestSupport.assertMapWithQuestionMarks("WithMap: {\n" +
                "  innerMap: {\n" +
                "    ? AUDUSD: AUDUSD1,\n" +
                "    ? USDPLN: USDPLN1\n" +
                "  },\n" +
                "}");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testMapReadAndWriteIntegers(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        @NotNull final YamlWire wire = new YamlWire(bytes);

        @NotNull final Map<Integer, Integer> expected = new HashMap<>();

        expected.put(1, 11);
        expected.put(2, 2);
        expected.put(3, 3);

        wire.writeDocument(false, o ->
                o.write(() -> "example").map(expected));

        assertEquals("--- !!data\n" +
                "example: {\n" +
                "  ? !int 1: !int 11,\n" +
                "  ? !int 2: !int 2,\n" +
                "  ? !int 3: !int 3\n" +
                "}\n", Wires.fromSizePrefixedBlobs(bytes), "integer map should serialize with type tags for keys and values");
        @NotNull final Map<Object, Object> actual = new HashMap<>();
        wire.readDocument(null, c -> {
            @Nullable Map m = c.read(() -> "example").marshallableAsMap(Object.class, Object.class, actual);
            assertEquals(m, expected, "deserialized integer map should match original");
        });

        wire.reset();
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testMapReadAndWriteMarshable(boolean usePadding) {
        initYamlWireTest(usePadding);
        WireMapTestSupport.assertMarshallableMap(YamlWire::new);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testException(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(usePadding);
        WireTestSupport.assertExceptionRoundTrip(wire, "net.openhft.chronicle.wire.YamlWireTest");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testEnum(boolean usePadding) {
        initYamlWireTest(usePadding);
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class, "WireType");

        @NotNull Wire wire = createWire();
        WireTestSupport.assertWireTypeRoundTrip(wire, "\"\": !WireType BINARY\n" +
                "\"\": !WireType TEXT\n" +
                "\"\": !WireType RAW\n");
    }


    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testLZWCompressionAsText(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();
        WireTestSupport.assertLzwCompressionAsText(wire, Bytes::allocateElasticDirect);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testStringArrays(boolean usePadding) {
        initYamlWireTest(usePadding);
        WireCollectionTestSupport.assertStringArraysRoundTrip(this::createWire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testStringList(boolean usePadding) {
        initYamlWireTest(usePadding);
        WireCollectionTestSupport.assertStringListRoundTrip(this::createWire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void fromList(boolean usePadding) {
        initYamlWireTest(usePadding);
        for (String text : new String[]{
                "[a, b, c]",
                "[ 'a', 'b', 'c' ]",
                "[ \"a\", \"b\", \"c\" ]"
        }) {
            @NotNull Wire wire = createWire();
            wire.bytes().append(text);
            @Nullable List<String> list = wire.read().object(List.class);
            assertEquals(Arrays.asList("a", "b", "c"), list, "list should deserialize from various YAML array formats");
        }
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testStringSet(boolean usePadding) {
        initYamlWireTest(usePadding);
        WireCollectionTestSupport.assertStringSetRoundTrip(this::createWire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testStringMap(boolean usePadding) {
        initYamlWireTest(usePadding);
        assertTrue(WireMapTestSupport.writeAndReadStringMap(YamlWire::new), "string map should serialize and deserialize correctly via YamlWire");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testYNestedDecode(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull String s = "cluster: {\n" +
                "  host1: {\n" +
                "     hostId: 1,\n" +
                // "     name: one,\n" +
                "  },\n" +
                "  host2: {\n" +
                "     hostId: 2,\n" +
                "  },\n" +
                "#  host3: {\n" +
                "#     hostId: 3,\n" +
                "#  },\n" +
                "  host4: {\n" +
                "     hostId: 4,\n" +
                "  },\n" +
                "}" +
                "cluster2: {\n" +
                "    host21: {\n" +
                "       hostId: 21\n" +
                "    }\n" +
                "}\n";
        assertEquals("DIRECTIVES_END \n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT cluster\n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT host1\n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT hostId\n" +
                "TEXT 1\n" +
                "MAPPING_END \n" +
                "MAPPING_KEY \n" +
                "TEXT host2\n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT hostId\n" +
                "TEXT 2\n" +
                "MAPPING_END \n" +
                "COMMENT host3: {\n" +
                "COMMENT hostId: 3,\n" +
                "COMMENT },\n" +
                "MAPPING_KEY \n" +
                "TEXT host4\n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT hostId\n" +
                "TEXT 4\n" +
                "MAPPING_END \n" +
                "MAPPING_END \n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT cluster2\n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT host21\n" +
                "MAPPING_START \n" +
                "MAPPING_KEY \n" +
                "TEXT hostId\n" +
                "TEXT 21\n" +
                "MAPPING_END \n" +
                "MAPPING_END \n" +
                "MAPPING_END \n" +
                "MAPPING_END \n" +
                "DOCUMENT_END \n", doTest("=" + s), "nested YAML structure should tokenize correctly with comments preserved");

        ObjIntConsumer<String> results = EasyMock.createMock(ObjIntConsumer.class);
        results.accept("host1", 1);
        results.accept("host2", 2);
        results.accept("host4", 4);
        replay(results);
        @NotNull YamlWire wire = YamlWire.from(s);
        wire.read(() -> "cluster").marshallable(v -> {
                    @NotNull StringBuilder sb = new StringBuilder();
                    while (wire.hasMore()) {
                        wire.readEventName(sb)
                                .marshallable(m ->
                                        m.read(() -> "hostId")
                                                .int32(sb.toString(), results));
                    }
                }
        );
        verify(results);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void writeNull(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();
        String written = WireNullTestSupport.writeNulls(wire, w -> w.getValueOut().object(null), Circle.class);
        assertEquals("!!null \"\"\n" +
                "!!null \"\"\n" +
                "!!null \"\"\n" +
                "!!null \"\"\n", written, "null values should serialize as YAML null type");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testAllChars(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();

        WireTestSupport.assertAllCharsRoundTrip(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void readDemarshallable(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = new YamlWire(allocateElasticOnHeap())
                .useBinaryDocuments();

        WireTestSupport.writeDemarshallable(wire);
        assertEquals(WireTestSupport.expectedDemarshallableBlob(), Wires.fromSizePrefixedBlobs(wire.bytes()), "demarshallable should serialize to expected YAML blob format");
        WireTestSupport.assertDemarshallableRead(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testByteArrayValueWithRealBytesNegative(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();

        WireTestSupport.assertByteArrayValueWithSwapLeaf(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testByteArray(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();

        WireByteArrayDocSupport.assertByteArrayDocuments(wire, usePadding);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testObjectKeys(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Map<MyMarshallable, String> map = new LinkedHashMap<>();
        map.put(new MyMarshallable("key1"), "value1");
        map.put(new MyMarshallable("key2"), "value2");

        @NotNull Wire wire = createWire();
        @NotNull final MyMarshallable parent = new MyMarshallable("parent");
        wire.writeDocument(false, w -> w.writeEvent(MyMarshallable.class, parent).object(map));

        assertEquals("--- !!data\n" +
                        "? { MyField: parent }: {\n" +
                        "  ? !net.openhft.chronicle.wire.MyMarshallable { MyField: key1 }: value1,\n" +
                        "  ? !net.openhft.chronicle.wire.MyMarshallable { MyField: key2 }: value2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire.bytes()), "map with Marshallable keys should serialize with type information");

        wire.readDocument(null, w -> {
            Map<MyMarshallable, Map> map1 = w.getValueIn().marshallableAsMap(MyMarshallable.class, Map.class);
            MyMarshallable mm = map1.keySet().iterator().next();
            assertEquals(parent.toString(), mm.toString(), "deserialized key should match parent toString");
            assertEquals(parent, mm, "deserialized key should equal parent object");
            @Nullable final Map map2 = map1.values().iterator().next();
            assertEquals(map, map2, "deserialized nested map should match original");
        });
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void writeUnserializable(boolean usePadding) {
        initYamlWireTest(usePadding);
        assertThrows(IllegalArgumentException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0);

            System.out.println(WireType.YAML_ONLY.asString(Thread.currentThread()));
        });
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void writeUnserializable2(boolean usePadding) {
        initYamlWireTest(usePadding);
        assertThrows(IllegalArgumentException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0);

            @NotNull Socket s = new Socket();
            System.out.println(WireType.YAML_ONLY.asString(s));
        });
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void writeUnserializable3(boolean usePadding) throws IOException {
        initYamlWireTest(usePadding);
        assertThrows(IllegalArgumentException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0);

            SocketChannel sc = SocketChannel.open();
            System.out.println(WireType.YAML_ONLY.asString(sc));
        });
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void writeCharacter(boolean usePadding) {
        initYamlWireTest(usePadding);
        @NotNull Wire wire = createWire();
        WireCharacterTestSupport.assertCharacterRoundTrip(wire, true);
    }


    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testStringArray(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        WireStringArrayTestSupport.assertStringArrayRoundTrip(this::createWire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testSetBytesAfterDeserialization(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        BytesWrapper bw = Marshallable.fromString("!net.openhft.chronicle.wire.YamlWireTest$BytesWrapper {\n" +
                "  bytes: \"\"\n" +
                "}\n");
        bw.bytes("");
        bw.bytes("hi");
        bw.bytes("hello");
        assertEquals("!net.openhft.chronicle.wire.YamlWireTest$BytesWrapper {\n" +
                "  bytes: hello\n" +
                "}\n", bw.toString(), "BytesWrapper should serialize with latest bytes value");
        bw.bytes.releaseLast();
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testArrayTypes(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire();
        wire.bytes().append("a: !type byte[]\n" +
                "b: !type String[]\n" +
                "c: hi");

        assertEquals(byte[].class, wire.read("a").typeLiteral(), "type literal should parse byte[] class");
        assertEquals(String[].class, wire.read("b").typeLiteral(), "type literal should parse String[] class");
        assertEquals("hi", wire.read("c").text(), "text field should parse correctly after type literals");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testArrayTypes1(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire();
        wire.bytes().append("a: !type \"[B\", b: !type \"String[]\", c: hi");

        assertEquals(byte[].class, wire.read("a").typeLiteral(), "type literal should parse byte[] from JVM notation [B");
        assertEquals(String[].class, wire.read("b").typeLiteral(), "type literal should parse String[] from quoted notation");
        assertEquals("hi", wire.read("c").text(), "text field should parse correctly after quoted type literals");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testArrayTypes2(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire();
        wire.bytes().append("a: [ !type byte[] ], b: !type String[], c: hi");

        assertEquals(String[].class, wire.read("b").typeLiteral(), "type literal should parse String[] class");
        Collection<Class> classes = wire.read("a").typedMarshallable();
        assertArrayEquals(new Class[]{byte[].class}, classes.toArray(), "type array should deserialize to collection of Class objects");
        assertEquals("hi", wire.read("c").text(), "text field should parse correctly after type array");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void readMarshallableAsEnum(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire();
        ClassAliasPool.CLASS_ALIASES.addAlias(YWTSingleton.class);
        wire.bytes().append("a: !YWTSingleton INSTANCE,\n" +
                "b: !YWTSingleton INSTANCE\n");
        assertEquals(YWTSingleton.INSTANCE, wire.read("a")
                .object(), "singleton enum should deserialize to INSTANCE");
        assertEquals(YWTSingleton.INSTANCE, wire.read("b").object(), "repeated singleton enum should deserialize to same INSTANCE");

    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void nestedWithEnumSet(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final Wire wire = createWire();
        YNestedWithEnumSet n = new YNestedWithEnumSet();
        n.list.add(new WithEnumSet("none"));
        n.list.add(new WithEnumSet("one", EnumSet.of(TimeUnit.DAYS)));
        n.list.add(new WithEnumSet("two", EnumSet.of(TimeUnit.DAYS, TimeUnit.HOURS)));
        wire.write("hello")
                .object(YNestedWithEnumSet.class, n);
        assertEquals("hello: {\n" +
                "  list: [\n" +
                "    { name: none },\n" +
                "    { name: one, timeUnits: [ DAYS ] },\n" +
                "    { name: two, timeUnits: [ HOURS, DAYS ] }\n" +
                "  ]\n" +
                "}\n", wire.toString(), "nested object with EnumSet should serialize list with enum values");

        YNestedWithEnumSet a = wire.read("hello")
                .object(YNestedWithEnumSet.class);
        assertEquals(n.toString(), a.toString(), "deserialized nested object toString should match original");
        assertEquals(n, a, "deserialized nested object should equal original");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testDoublePrecisionOverYamlWire(boolean usePadding) {
        initYamlWireTest(usePadding);
        final Bytes<?> bytes = allocateElasticOnHeap();

        final Wire wire = WireType.YAML.apply(bytes);
        final double d = 0.000212345678901;
        wire.getValueOut().float64(d);

        final YamlWire wire2 = YamlWire.from(bytes.toString());
        final double d2 = wire2.getValueIn().float64();

        Assertions.assertEquals(d, d2, 0, "high-precision double should round-trip without loss");
        bytes.releaseLast();
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void readsComment(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire();
        String actual = WireCommentTestSupport.exerciseReadComments(wire);

        assertEquals("one\n" +
                "two\n" +
                "dto: !net.openhft.chronicle.wire.BinaryWireTest$DTO {\n" +
                "  text: text\n" +
                "}\n" +
                "\n" +
                "three\n", actual, "comments should be preserved and read correctly in YAML output");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void readMetaData(boolean usePadding) {
        initYamlWireTest(usePadding);
        WireTestSupport.assertReadMetaData(wire);
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testNestedListInterleavedComments(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        YamlWireTest.StringArray obj = WireType.YAML.fromString(YamlWireTest.StringArray.class,
                "     # first\n" +
                        "{\n" +
                        "     # more\n" +
                        "  strings: [\n" +
                        "     # foo\n" +
                        "     'bar',\n" +
                        "     # baz\n" +
                        "     'quux'\n" +
                        "     # thud\n" +
                        "  ]\n" +
                        "     # xyzzy\n" +
                        "}\n" +
                        "     # fin\n");

        assertArrayEquals(new String[]{"bar", "quux"}, obj.strings, "string array should deserialize correctly with interleaved comments");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testListInterleavedComments(boolean usePadding) {
        initYamlWireTest(usePadding);
        List<String> obj = WireType.YAML.fromString(
                "     # first\n" +
                        "[\n" +
                        "     # foo\n" +
                        "     'bar',\n" +
                        "     # baz\n" +
                        "     'quux'\n" +
                        "     # thud\n" +
                        "]\n" +
                        "     # fin\n");

        assertEquals(Arrays.asList("bar", "quux"), obj, "list should deserialize correctly with interleaved comments");
    }

    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void putData(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Data data = new Data();
        data.timeNS = (long) 1.6e18;
        data.bytes = Bytes.from("zzz");
        data.data = new byte[2];

        wire.methodWriter(PutData.class)
                .put(Bytes.from("hi"), data);

        Wire wire2 = Wire.newYamlWireOnHeap();
        wire.methodReader(wire2.methodWriter(PutData.class))
                .readOne();
        assertEquals("put: [\n" +
                "  hi,\n" +
                "  {\n" +
                "    timeNS: 2020-09-13T12:26:40,\n" +
                "    bytes: zzz,\n" +
                "    data: !!binary AAA=\n" +
                "  }\n" +
                "]\n" +
                "...\n", wire2.toString(), "method call should serialize with timestamp, bytes, and binary data");
    }

    // Test for Empty YAML Document
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testEmptyYamlDocument(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire();
        wire.bytes().append("");
        assertFalse(wire.readingDocument().isPresent(), "empty document should not be present");
    }

    // Test for Large YAML Documents
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "usePadding={0}")
    public void testLargeYamlDocument(boolean usePadding) {
        initYamlWireTest(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Wire wire = createWire();
        StringBuilder largeYaml = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeYaml.append("key").append(i).append(": value").append(i).append('\n');
        }
        wire.bytes().append(largeYaml.toString());
        for (int i = 0; i < 10000; i++) {
            assertEquals("value" + i, wire.read("key" + i).text(), "large YAML document should read key" + i + " correctly");
        }
    }

    // Test for Special Characters in Strings
    @MethodSource("wireTypes")
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @ParameterizedTest(name = "usePadding={0}")
    public void testSpecialCharactersInStrings(boolean usePadding) {
        initYamlWireTest(usePadding);
        Wire wire = createWire();
        wire.bytes().append("text: \"Line1\\nLine2\\tTabbed\\u263A\"");
        assertEquals("Line1\nLine2\tTabbed\u263A", wire.read("text").text(), "special characters including newline, tab, and unicode should parse correctly");
    }


    enum YWTSingleton {
        INSTANCE
    }

    interface PutData {
        void put(Bytes<?> key, Data data);
    }

    private static class FieldWithComment extends SelfDescribingMarshallable {
        @Comment("a comment where the value=%s")
        String field;
        // String field2;
    }

    private static class FieldWithComment2 extends SelfDescribingMarshallable {
        @Comment("a comment where the value=%s")
        String field;
        String field2;
    }

    static class TwoFields extends AbstractMarshallableCfg {
        String b;
        int d;
        int notThere;

        final transient Map<String, Object> others = new LinkedHashMap<>();

        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            others.put(event.toString(), valueIn.object());
        }
    }

    static class StringArray implements Marshallable {
        String[] strings;
    }

    static class BytesWrapper extends SelfDescribingMarshallable {
        @NotNull
        final
        Bytes<?> bytes = allocateElasticDirect();

        void bytes(@NotNull CharSequence cs) {
            bytes.clear();
            bytes.append(cs);
        }
    }

    static class YNestedWithEnumSet extends SelfDescribingMarshallable {
        final List<WithEnumSet> list = new ArrayList<>();
    }

    static class Data extends SelfDescribingMarshallable {
        @NanoTime
        long timeNS;
        Bytes bytes;
        byte[] data;
    }

    private static class Circle implements Marshallable {
    }
}
