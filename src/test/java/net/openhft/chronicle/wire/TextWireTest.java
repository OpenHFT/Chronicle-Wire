/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.Monitorable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticDirect;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"rawtypes", "unchecked", "try", "serial", "deprecation"})
public class TextWireTest extends AbstractWireTest {

    // Create a new TextWire instance with an elastic heap allocated buffer
    private static final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
    private Bytes<?> bytes;

    @BeforeEach
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    // Test to check if white space within type specifications is handled correctly.
    @Test
    public void fromList() {
        for (String text : new String[]{
                "[a , b\n, c]",
                "[ 'a'\n, 'b' , 'c' ]",
                "[ \"a\" , \"b\" ,\n\"c\"\n]"
        }) {
            @NotNull Wire wire = createWire();
            wire.bytes().append(text);
            @Nullable List<String> list = wire.read().object(List.class);
            assertEquals(Arrays.asList("a", "b", "c"), list, "list should deserialize from text format with whitespace variations");
        }
    }

    @Test
    public void testWhiteSpaceInType() {
        try {
            // Deserialize from string and check if the object is correctly formed
            Object o = Marshallable.fromString("key: !" + DTO.class.getName() + " {\n" +
                    "  type:            !type               String\n" +
                    "}\n");

            assertNotNull(o, "DTO should deserialize successfully with whitespace in type declaration");

        } catch (Exception e) {
            Assertions.fail();
        }
    }

    // Test handling of Bytes data type in TextWire.
    @Test
    public void testBytes() {
        final Wire wire = createWire();
        @NotNull byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;

        // Write different bytes sequences to the wire
        WireBytesTestSupport.exerciseBytesRoundTrip(wire, WireBytesTestSupport.helloBytes(), WireBytesTestSupport.quoteBytes(), allBytes);

        // Read back the bytes sequences and validate their content
        @NotNull Bytes<?> allBytes2 = allocateElasticOnHeap();
        WireBytesTestSupport.assertBytesRoundTrip(wire, allBytes, allBytes2);
    }

    // Test handling of comments in TextWire.
    @Test
    public void comment() {
        final Wire wire = createWire();
        wire.writeComment("\thi: omg");
        wire.write("hi").text("there");
        assertEquals("there", wire.read("hi").text(), "text field should read correctly after comment is written");
    }

    // Test handling of type specification instead of an actual field in TextWire.
    @Test
    public void testTypeInsteadOfField() {
        Wire wire = TextWire.from("!!null \"\"");
        StringBuilder sb = new StringBuilder();
        wire.read(sb).object(Object.class);
        assertEquals(0, sb.length(), "field name should be empty when reading null type");
    }

    // Test serialization with fields accompanied by comments in TextWire.
    @Test
    public void testFieldWithComment() {
        FieldWithComment f = new FieldWithComment();
        f.field = "hello world";
        Assertions.assertEquals("!net.openhft.chronicle.wire.TextWireTest$FieldWithComment {\n" +
                "  field: hello world, \t\t# a comment where the value=hello world\n" +
                "}\n", Marshallable.$toString(f));
    }

    // Test serialization with multiple fields accompanied by comments in TextWire.
    @Test
    public void testFieldWithComment2() {
        FieldWithComment2 f = new FieldWithComment2();
        f.field = "hello world";
        Assertions.assertEquals("!net.openhft.chronicle.wire.TextWireTest$FieldWithComment2 {\n" +
                "  field: hello world, \t\t# a comment where the value=hello world\n" +
                "  field2: !!null \"\"\n" +
                "}\n", Marshallable.$toString(f));
    }

    // Test correct handling of comments placed after string values in TextWire.
    @Test
    public void testCommentAfterString() {
        Map<String, Object> o = Marshallable.fromString("{\n" +
                "  pattern: '@Symbol =~ \"[A-L].*\"', # quoted\n" +
                "  policy: ROUND_ROBIN, # unquoted\n" +
                "  routes: [ \"INT1\" ] # terminating list\n" +
                "}");

        assertEquals("ROUND_ROBIN", o.get("policy"), "unquoted value should parse correctly after comment");
        assertEquals(Collections.singletonList("INT1"), o.get("routes"), "list value should parse correctly with terminating comment");
        assertEquals("@Symbol =~ \"[A-L].*\"", o.get("pattern"), "quoted value should parse correctly after comment");
    }

    // Test to ensure that unexpected fields in the serialized string are properly handled and deserialized.
    @Test
    public void handleUnexpectedFields() {
        // Deserialize a string with more fields than the TwoFields class has.
        // Fields "d", "e", and "f" are not part of TwoFields, and should be collected in the "others" field.
        TwoFields tf = Marshallable.fromString("!" + TwoFields.class.getName() + " {" +
                "a : 1 ,\n" +
                "b\t : two,\n" +
                "c: three,\n" +
                "d: 44 , \n" +
                "e: also,\n" +
                "f: at the end\n" +
                "}");
        // Check if the unexpected fields are correctly populated in the "others" field.
        assertEquals("a=1\n" +
                        "c=three\n" +
                        "e=also\n" +
                        "f=at the end",
                asProperties(tf.others), "unexpected fields should be captured in others map");

        // Repeat the above steps with different unexpected fields.
        TwoFields tf2 = Marshallable.fromString("!" + TwoFields.class.getName() + " {" +
                "a: 1,\n" +
                "b: two,\n" +
                "c: three,\n" +
                "d: 44,\n" +
                "e: also,\n" +
                "}");
        assertEquals("a=1\n" +
                        "c=three\n" +
                        "e=also",
                asProperties(tf2.others), "unexpected fields should be captured when interleaved with known fields");

        // Check case sensitivity of field names
        TwoFields tf3 = Marshallable.fromString("!" + TwoFields.class.getName() + " {" +
                "A: 1,\n" +
                "B: two,\n" +
                "C: three,\n" +
                "D: 44,\n" +
                "E: also,\n" +
                "}");
        assertEquals("a=1\n" +
                        "c=three\n" +
                        "e=also",
                asProperties(tf3.others), "field names should be matched case-insensitively");
    }

    // Utility method to convert a map to a string representation in properties format.
    private String asProperties(Map<String, Object> map) {
        return map.entrySet().stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    // Test to check the license validation for different WireTypes.
    @Test
    public void licenseCheck() {
        // Verify that TEXT WireType doesn't require any license check.
        WireType.TEXT.licenceCheck();
        assertTrue(WireType.TEXT.isAvailable(), "TEXT wire type should be available without license check");
    }

    // Test to ensure that objects with TreeMap fields are correctly serialized and deserialized.
    @Test
    public void writeObjectWithTreeMap() {
        WireMapTestSupport.assertObjectWithTreeMap(WireType.TEXT::apply);
    }

    // Test to ensure a serialized string with a nested map structure can be deserialized into a Map.
    @Test
    public void testFromString() {
        @Nullable Object w = WireType.TEXT.fromString("changedRow: {\n" +
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
        // Ensure the deserialized object is an instance of Map.
        assertInstanceOf(Map.class, w, "deserialized object should be a Map when reading nested map structure");
    }

    // Test to verify deserialization of integer values presented in hexadecimal format.
    @Test
    public void testFromString2() {
        // Iterate through integers from 0 to 256.
        for (int i = 0; i <= 256; i++) {
            // Deserialize a string containing the integer in uppercase and lowercase hexadecimal format.
            Wire w = TextWire.from(
                    "data: 0x" + Integer.toHexString(i).toUpperCase() + ",\n" +
                            "data2: 0x" + Integer.toHexString(i).toLowerCase());
            // Verify that both deserialized values match the original integer.
            assertEquals(i, w.read("data").int64(), "uppercase hexadecimal value should parse correctly to int64");
            assertEquals(i, w.read("data2").int64(), "lowercase hexadecimal value should parse correctly to int64");
        }
    }

    // Test to serialize a data structure in BINARY format and then try to convert it to TEXT format.
    @Test
    public void testWriteToBinaryAndTriesToConvertToText() {

        Bytes<?> b = allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(b);
        wire.usePadding(true);

        // Define the data structure.
        @NotNull Map<String, String> data = Collections.singletonMap("key", "value");

        @NotNull HashMap map = new HashMap();
        map.put("some", data);
        map.put("some-other", data);

        // Write the map to the wire.
        try (DocumentContext dc = wire.writingDocument()) {
            wire.write("map").object(map);
        }

        // Convert the binary blob into a string representation.
        final String textYaml = Wires.fromSizePrefixedBlobs(b);
        // Deserialize the TEXT into an object and verify its structure.
        @Nullable Object o = WireType.TEXT.fromString(textYaml);
        Assertions.assertEquals("{map={some={key=value}, some-other={key=value}}}", o.toString(), "binary wire should convert to text format preserving nested map structure");

        b.releaseLast();
    }

    // Test to ensure calling the 'write()' method multiple times will produce the expected string.
    @Test
    public void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        assertEquals("\"\": \"\": \"\": ", wire.toString(), "multiple write() calls should produce empty key-value pairs");
    }

    @NotNull
    @Override
    protected Wire createWire() {
        wire.reset();
        bytes = wire.bytes();
        return wire;
    }

    // Test to validate reading from the wire
    @Test
    @Override
    public void testRead() {
        @NotNull Wire wire = createWire();

        // Write values to the wire
        WireReadTestSupport.writeStandardFields(wire);
        wire.read();
        wire.read();
        wire.read();
        assertEquals(1, wire.bytes().readRemaining(), "wire should have 1 byte remaining after reading 3 fields without keys");
        wire.read();
    }

    // Test to validate reading using specific keys from the wire
    @Test
    public void testRead1() {
        @NotNull Wire wire = createWire();

        // Write values to the wire
        WireReadTestSupport.writeStandardFields(wire);

        // Read values using specific key. If the key is blank, it matches any key.
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        assertEquals(0, wire.bytes().readRemaining(), "wire should be fully consumed after reading 3 fields with matching keys");
        wire.read();
    }

    // Test to validate reading values into a StringBuilder
    @Test
    public void testRead2() {
        @NotNull Wire wire = createWire();

        // Write values to the wire
        wire.write();
        wire.write(BWKey.field1);
        @NotNull String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name1);

        // Read values into StringBuilder. If the key is blank, it matches any key.
        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name);
        assertEquals(0, name.length(), "first field name should be empty when reading into StringBuilder");

        name.setLength(0);
        wire.read(name);
        assertEquals(BWKey.field1.name(), name.toString(), "second field name should match BWKey.field1 when reading into StringBuilder");

        name.setLength(0);
        wire.read(name);
        assertEquals(name1, name.toString(), "third field name should match long field name when reading into StringBuilder");

        assertEquals(1, wire.bytes().readRemaining(), "wire should have 1 byte remaining after reading 3 field names into StringBuilder");
        wire.read();
    }

    // Test to validate writing and reading 8-bit integers from the wire
    @Test
    public void int8() {
        @NotNull Wire wire = createWire();

        WireSmallIntTestSupport.writeInt8Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readInt8Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "all int8 values should be consumed after reading");
        wire.read();
    }

    // Test case to validate writing and reading 16-bit integers from the wire
    @Test
    public void int16() {
        @NotNull Wire wire = createWire();

        WireSmallIntTestSupport.writeInt16Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readInt16Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "all int16 values should be consumed after reading");
        wire.read();
    }

    // Test case to validate writing and reading unsigned 8-bit integers from the wire
    @Test
    public void uint8() {
        @NotNull Wire wire = createWire();

        WireSmallIntTestSupport.writeUint8Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readUint8Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "all uint8 values should be consumed after reading");
        wire.read();
    }

    // Test case to validate writing and reading unsigned 16-bit integers from the wire
    @Test
    public void uint16() {
        @NotNull Wire wire = createWire();

        WireSmallIntTestSupport.writeUint16Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readUint16Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "all uint16 values should be consumed after reading");
        wire.read();
    }

    // Test case to validate writing and reading unsigned 32-bit integers from the wire
    @Test
    public void uint32() {
        @NotNull Wire wire = createWire();

        WireSmallIntTestSupport.writeUint32Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readUint32Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "all uint32 values should be consumed after reading");
        wire.read();
    }

    // Test case to validate writing and reading 32-bit integers from the wire
    @Test
    public void int32() {
        @NotNull Wire wire = createWire();

        WireSmallIntTestSupport.writeInt32Triplet(wire);

        WireSmallIntTestSupport.expectTextLayout(wire, "{=1, field1=2, Test=3}", "\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n");

        WireSmallIntTestSupport.readInt32Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "all int32 values should be consumed after reading");
        wire.read();
    }

    // Test case to validate writing and reading 64-bit integers from the wire
    @Test
    public void int64() {
        @NotNull Wire wire = createWire();

        // Write 64-bit integers to the wire with various keys
        WireNumericTestSupport.writeInt64s(wire);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString(), "int64 values should serialize to text format with field names");

        // Read the 64-bit integers from the wire and validate their values
        WireNumericTestSupport.assertInt64sRead(wire, false);
    }

    // Test case for writing and reading 64-bit floating point numbers
    @Test
    public void float64() {
        @NotNull Wire wire = createWire();

        // Write 64-bit floating point numbers to the wire with various keys
        WireNumericTestSupport.writeFloat64s(wire);

        // Validate the wire's string format
        assertEquals("\"\": 1.0\n" +
                "field1: 2.0\n" +
                "Test: 3.0\n", wire.toString(), "float64 values should serialize with .0 suffix in text format");

        // Validate using SnakeYAML parser
        expectWithSnakeYaml("{=1.0, field1=2.0, Test=3.0}", wire);

        // Read values using a custom float wrapper and validate
        WireNumericTestSupport.assertFloat64sRead(wire);
    }

    // Test case for writing and reading text values
    @Test
    public void text() {
        @NotNull Wire wire = createWire();

        // Write text values to the wire
        @NotNull String name = "Long field name which is more than 32 characters, \\ \nBye";
        WireStringTestSupport.writeStrings(wire, name);
        // Validate using SnakeYAML parser and the expected string format
        expectWithSnakeYaml("{=Hello, field1=world, Test=Long field name which is more than 32 characters, \\ \n" +
                "Bye}", wire);
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, \\\\ \\nBye\"\n", wire.toString(), "text strings should escape special characters and quote long values");

        // Read the texts back and validate
        WireStringTestSupport.assertReadStrings(wire, name);

        assertEquals(0, bytes.readRemaining(), "all text values should be consumed after reading");
        // Check it's safe to read too much.
        wire.read();
    }

    // Test case for writing and reading type prefixes
    @Test
    public void type() {
        @NotNull Wire wire = createWire();

        // Write type prefixes to the wire
        wire.write().typePrefix("MyType");
        wire.write(BWKey.field1).typePrefix("AlsoMyType");
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write(() -> "Test").typePrefix(name1);

        // Add a comment for visual separation
        wire.writeComment("");
        // TODO fix how types are serialized.
        // expectWithSnakeYaml(wire, "{=1, field1=2, Test=3}");
        assertEquals("\"\": !MyType " +
                "field1: !AlsoMyType " +
                "Test: !" + name1 + " # \n", wire.toString(), "type prefixes should serialize with ! marker before type name");

        // Read the types back and validate
        Stream.of("MyType", "AlsoMyType", name1).forEach(e ->
                wire.read().typePrefix(e, Assertions::assertEquals));

        assertEquals(0, bytes.readRemaining(), "all type prefix values should be consumed after reading");
        // Check it's safe to read too much.
        wire.read();
    }

    // Test case for working with custom types having empty body
    @Test
    public void testTypeWithEmpty() {
        // Expect an exception with a specific message when processing an object with missing content
        expectException("Expected a {} but was blank for type class net.openhft.chronicle.wire.TextWireTest$NestedB");

        // Add type aliases for easier serialization/deserialization
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedA.class, NestedB.class);

        // Deserialize the string content to an object
        NestedA a = Marshallable.fromString("!NestedA {\n" +
                "  b: !NestedB,\n" +
                "  value: 12345\n" +
                "}");

        // Check the serialized form of the object matches the expected string format
        assertEquals("!NestedA {\n" +
                "  b: {\n" +
                "    field1: 0.0\n" +
                "  },\n" +
                "  value: 12345\n" +
                "}\n", a.toString(), "empty type marker should create object with default field values");
    }

    // Test case for working with single quoted custom types
    @Test
    public void testSingleQuote() {
        // Expect an exception with a specific message when processing an object with missing content
        expectException("Expected a {} but was blank for type class net.openhft.chronicle.wire.TextWireTest$NestedB");

        // Add a type alias
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedA.class);

        // Deserialize string content to an object
        NestedA a = Marshallable.fromString("!NestedA {\n" +
                "  b: !NestedB,\n" +
                "  value: 12345\n" +
                "}");

        assertNotNull(a, "object should deserialize successfully with empty type body");
    }

    // Test case for writing and reading boolean values
    @Test
    public void testBool() {
        @NotNull Wire wire = createWire();

        WirePrimitiveTestSupport.assertBooleanRoundTrip(wire);
    }

    // Test case for writing and reading 32-bit floating point numbers
    @Test
    public void testFloat32() {
        @NotNull Wire wire = createWire();

        WirePrimitiveTestSupport.assertFloat32RoundTrip(wire, this);
    }

    // Test case for writing and reading LocalTime values
    @Test
    public void testTime() {
        @NotNull Wire wire = createWire();

        LocalTime now = LocalTime.now();

        WirePrimitiveTestSupport.writeTimes(wire, now);

        assertEquals(WirePrimitiveTestSupport.expectedTimeString(now), bytes.toString(), "LocalTime should serialize in ISO-8601 format");

        WirePrimitiveTestSupport.assertTimes(wire, now);
    }


    // Test case for working with LocalDate values
    @Test
    public void testDate() {
        @NotNull Wire wire = createWire();

        WireTemporalTestSupport.assertLocalDates(wire);
    }

    // Test case for working with UUID values
    @Test
    public void testUuid() {
        @NotNull Wire wire = createWire();

        WireTemporalTestSupport.assertUuids(wire);
    }

    @Test
    public void testTypeWithoutSpace() {
        @NotNull Wire wire = createWire();

        WireTestSupport.assertTypeWithoutSpace(wire);
    }

    @Test
    public void testNANValue() {
        @NotNull Wire wire = createWire();

        WireTestSupport.assertNanValues(wire);
    }

    @Test
    public void testABCDBytes() {
        @NotNull Wire wire = createWire();

        WireAbcTestSupport.assertAbcdBytes(wire, false);
    }

    // Test the string building behavior for ABC objects with Wire.
    @Test
    public void testABCStringBuilder() {
        @NotNull Wire wire = createWire();

        WireAbcTestSupport.assertAbcStringBuilder(wire, Arrays.asList("This is an A", "This is a B"));
    }

    // Test reading and writing of a string map with Wire.
    @Test
    public void testMapReadAndWriteStrings() {
        @NotNull final Bytes<?> localBytes = allocateElasticOnHeap();
        try {
            @NotNull final Map<String, String> expected = new LinkedHashMap<>();
            expected.put("hello", "world");
            expected.put("hello1", "world1");
            expected.put("hello2", "world2");

            @NotNull final Wire wire = WireType.TEXT.apply(localBytes);

            wire.writeDocument(false, o -> o.writeEventName(() -> "example").map(expected));

            assertEquals("--- !!data\n" +
                            "example: {\n" +
                            "  hello: world,\n" +
                            "  hello1: world1,\n" +
                            "  hello2: world2\n" +
                            "}\n",
                    Wires.fromSizePrefixedBlobs(localBytes),
                    "string map should serialize as YAML document with field names");

            @NotNull final Map<String, String> actual = new LinkedHashMap<>();
            wire.readDocument(null, c -> c.read(() -> "example").marshallableAsMap(String.class, String.class, actual));
            assertEquals(expected, actual, "string map: roundtrip");
        } finally {
            localBytes.releaseLast();
        }
    }

    // Test behavior when using fields of type Bytes.
    // Note: This test is ignored due to unreleased bytes.
    @Test
    @Disabled("unreleased bytes")
    public void testBytesField() {
        DtoWithBytesField dto = new DtoWithBytesField(), dto2 = null;
        byte[] binaryData = {1, 2, 3, 4};
        dto.bytes = Bytes.wrapForRead(binaryData);
        dto.another = 123L;

        try {
            // Convert the DTO to string and back, and assert equality
            String cs = dto.toString();
            dto2 = Marshallable.fromString(cs);
            assertEquals(cs, dto2.toString(), "BytesField should roundtrip through string serialization");
        } finally {
            // Ensure resources are cleaned up
            dto.bytes.releaseLast();
            dto2.bytes.releaseLast();
        }
    }


    // Test reading and writing a map with integer keys and values to/from a Wire.
    @Test
    public void testMapReadAndWriteIntegers() {
        // Create a byte store and wire to work with
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = WireType.TEXT.apply(bytes);

        // Populate the expected map
        @NotNull final Map<Integer, Integer> expected = new HashMap<>();

        expected.put(1, 11);
        expected.put(2, 2);
        expected.put(3, 3);

        // Write the map to wire
        wire.writeDocument(false, o ->
                o.write(() -> "example").map(expected));

        // Assert that the wire content matches expected format
        assertEquals("--- !!data\n" +
                "example: {\n" +
                "  ? !int 1: !int 11,\n" +
                "  ? !int 2: !int 2,\n" +
                "  ? !int 3: !int 3\n" +
                "}\n", Wires.fromSizePrefixedBlobs(bytes), "integer map should serialize with !int type markers for keys and values");

        // Read the map from wire and assert it matches the expected map
        @NotNull final Map<Integer, Integer> actual = new HashMap<>();
        wire.readDocument(null, c -> {
            @Nullable Map m = c.read(() -> "example").marshallableAsMap(Integer.class, Integer.class, actual);
            assertEquals(m, expected, "integer map should deserialize correctly from text format");
        });
    }

    // Test parsing a map within a map from a string
    @Test
    public void testMapInMap() {
        WireMapTestSupport.assertMapInMap("WithMap: {\n" +
                "  innerMap: {\n" +
                "    AUDUSD: AUDUSD1,\n" +
                "    USDPLN: USDPLN1\n" +
                "  },\n" +
                "}");
    }

    // Test parsing a map with question marks (indicating explicit keys) within another map from a string
    @Test
    public void testMapInMapWithQuestionMarks() {
        WireMapTestSupport.assertMapWithQuestionMarks("WithMap: {\n" +
                "  innerMap: {\n" +
                "    ? AUDUSD: AUDUSD1,\n" +
                "    ? USDPLN: USDPLN1\n" +
                "  },\n" +
                "}");
    }

    // Test reading and writing a map with Marshallable keys and values to/from a Wire.
    @Test
    public void testMapReadAndWriteMarshable() {
        WireMapTestSupport.assertMarshallableMap(WireType.TEXT::apply);
    }

    // Test writing an exception to a Wire and then reading it back.
    @Test
    public void testException() {
        WireTestSupport.assertExceptionRoundTrip(WireType.TEXT.apply(allocateElasticOnHeap()),
                "net.openhft.chronicle.wire.TextWireTest");
    }

    // Test writing an enum to a Wire and then reading it back.
    @Test
    public void testEnum() {
        // Register an alias for the WireType enum
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class, "WireType");

        // Create a wire and write several enum values to it
        @NotNull Wire wire = createWire();
        WireTestSupport.assertWireTypeRoundTrip(wire, "\"\": !WireType BINARY\n" +
                "\"\": !WireType TEXT\n" +
                "\"\": !WireType RAW\n");
    }



    // Test LZW compression of text strings written to a Wire.
    @Test
    public void testLZWCompressionAsText() {
        @NotNull Wire wire = createWire();
        WireTestSupport.assertLzwCompressionAsText(wire, Bytes::allocateElasticOnHeap);
    }

    // Test writing arrays of strings to a Wire and reading them back.
    @Test
    public void testStringArrays() {
        WireCollectionTestSupport.assertStringArraysRoundTrip(this::createWire);
    }

    // Test writing lists of strings to a Wire and reading them back.
    @Test
    public void testStringList() {
        WireCollectionTestSupport.assertStringListRoundTrip(this::createWire);
    }

    // Test writing sets of strings to a Wire and reading them back.
    @Test
    public void testStringSet() {
        WireCollectionTestSupport.assertStringSetRoundTrip(this::createWire);
    }

    // This test is for writing a Map<String, String> to the Wire and reading it back.
    // Currently, it's marked as ignored using the @Ignore annotation.
    @Test
    @Disabled
    public void testStringMap() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Create an empty map and write it to the wire
        @NotNull Map<String, String> noObjects = new HashMap();
        wire.write().object(noObjects);

        // Read the map from the wire and ensure it's empty
        @Nullable Map<String, String> map = wire.read().object(Map.class);
        assertEquals(0, map.size(), "empty map should deserialize with zero entries");

        // TODO we should not need to create a new wire.
        // wire = createWire();
        //
        // Set<String> threeObjects = new HashSet(Arrays.asList(new String[]{"abc", "def", "ghi"}));
        // wire.write().object(threeObjects);
        //
        // Set<String> list2 = wire.read()
        //         .object(Set.class);
        // assertEquals(3, list2.size());
        // assertEquals("[abc, def, ghi]", list2.toString());
    }

    // This test case demonstrates how to decode nested structures from a textual representation.
    @Test
    public void testNestedDecode() {
        final String s = "cluster: {\n" +
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

        // Mock an ObjIntConsumer to capture results
        ObjIntConsumer<String> results = EasyMock.createMock(ObjIntConsumer.class);

        // Set expectations on the mock
        results.accept("host1", 1);
        results.accept("host2", 2);
        results.accept("host4", 4);

        // Activate the mock
        replay(results);

        // Decode the string using TextWire
        @NotNull TextWire wire = TextWire.from(s);
        wire.read(() -> "cluster").marshallable(v -> {
                    @NotNull StringBuilder sb = new StringBuilder();
                    while (wire.hasMore()) {
                        wire.readEventName(sb).marshallable(m -> {
                            m.read(() -> "hostId").int32(sb.toString(), results);
                        });
                    }
                }
        );

        // Verify the mock was called as expected
        verify(results);
    }

    // Test writing null objects to the wire and reading them back.
    @Test
    public void writeNull() {
        @NotNull Wire wire = createWire();
        String text = WireNullTestSupport.writeNulls(wire, w -> w.write().object(null), Circle.class);
        assertFalse(text.isEmpty(), "null object should produce non-empty output");
        assertEquals(0, wire.bytes().readRemaining(), "null object bytes should be fully consumed after reading");
    }

    // Test to ensure all characters within the defined range are correctly written and read from a Wire
    @Test
    public void testAllChars() {
        @NotNull Wire wire = createWire();

        WireTestSupport.assertAllCharsRoundTrip(wire);
    }

    // Test reading of a demarshallable object from the Wire and ensuring its integrity
    @Test
    public void readDemarshallable() {
        @NotNull Wire wire = createWire();
        WireTestSupport.writeDemarshallable(wire);

        assertEquals("40000052", Integer.toUnsignedString(wire.bytes().readInt(0), 16), "document header should contain correct size prefix");
        assertEquals("!net.openhft.chronicle.wire.DemarshallableObject {\n" +
                "  name: test,\n" +
                "  value: 12345\n" +
                "}\n", wire.toString().substring(4), "demarshallable object should serialize with type and field names");

        assertEquals(WireTestSupport.expectedDemarshallableBlob(), Wires.fromSizePrefixedBlobs(wire.bytes()), "size-prefixed blob should match expected YAML format");

        WireTestSupport.assertDemarshallableRead(wire);
    }

    // Test writing and reading of a byte array with negative values to and from the Wire
    @Test
    public void testByteArrayValueWithRealBytesNegative() {
        @NotNull Wire wire = createWire();

        WireTestSupport.assertByteArrayValueWithSwapLeaf(wire);
    }

    // Test that ensures execution of 'testByteArrayValueWithRealBytesNegative' and then resets the wire and runs 'uint16'
    @Test
    public void two() {
        testByteArrayValueWithRealBytesNegative();
        wire.reset();
        assertEquals(0, wire.bytes().readRemaining(), "two: bytes empty after reset");
        uint16();
    }

    // Test for writing and reading byte arrays of various lengths to and from the Wire.
    @Test
    public void testByteArray() {
        final Wire wire = createWire();
        wire.usePadding(true);

        WireByteArrayDocSupport.assertByteArrayDocuments(wire, true);
    }

    // Test to ensure a map with custom marshallable keys is correctly written and read from the Wire.
    @Test
    public void testObjectKeys() {
        // Create a map with custom Marshallable objects as keys.
        @NotNull Map<MyMarshallable, String> map = new LinkedHashMap<>();
        map.put(new MyMarshallable("key1"), "value1");
        map.put(new MyMarshallable("key2"), "value2");

        // Initialize a new Wire instance.
        final Wire wire = createWire();

        // Disable padding for the Wire.
        wire.usePadding(false);

        // Define a custom Marshallable object as a parent.
        @NotNull final MyMarshallable parent = new MyMarshallable("parent");

        // Write the map to the Wire.
        wire.writeDocument(false, w -> w.writeEvent(MyMarshallable.class, parent).object(map));

        // Validate the written content on the Wire.
        assertEquals("--- !!data\n" +
                        "? { MyField: parent }: {\n" +
                        "  ? !net.openhft.chronicle.wire.MyMarshallable { MyField: key1 }: value1,\n" +
                        "  ? !net.openhft.chronicle.wire.MyMarshallable { MyField: key2 }: value2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire.bytes()), "map with marshallable keys should serialize with type markers and field names");

        // Read back the map from the Wire and verify its contents.
        wire.readDocument(null, w -> {
            MyMarshallable mm = w.readEvent(MyMarshallable.class);
            assertEquals(parent.toString(), mm.toString(), "marshallable key should deserialize with correct string representation");
            assertEquals(parent, mm, "marshallable key should deserialize equal to original");
            @Nullable final Map map2 = w.getValueIn()
                    .object(Map.class);
            assertEquals(map, map2, "map with marshallable keys should roundtrip correctly");
        });
    }

    // Test for attempting to serialize a non-serializable object (current thread).
    @Test
    public void writeUnserializable1() {
        assertThrows(IllegalArgumentException.class, () ->
                System.out.println(TEXT.asString(Thread.currentThread())));
    }

    // Test for attempting to serialize a non-serializable object (socket instance).
    @Test
    public void writeUnserializable2() {
        assertThrows(IllegalArgumentException.class, () -> {
            @NotNull Socket s = new Socket();
            System.out.println(TEXT.asString(s));
        });
    }

    // Test for attempting to serialize a non-serializable object (socket channel instance).
    @Test
    public void writeUnserializable3() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            SocketChannel sc = SocketChannel.open();
            System.out.println(TEXT.asString(sc));
        });
    }

    // Test to ensure characters are correctly written to and read back from the Wire.
    @Test
    public void writeCharacter() {
        final Wire wire = createWire();

        WireCharacterTestSupport.assertCharacterRoundTrip(wire, false);
    }


    // Test to verify the correct deserialization of String arrays from Wire.
    @Test
    public void testStringArray() {
        WireStringArrayTestSupport.assertStringArrayRoundTrip(this::createWire);
    }

    // Test to ensure bytes can be correctly set after deserialization from Wire.
    @Test
    public void testSetBytesAfterDeserialization() {
        // Deserialize a BytesWrapper instance from a string representation.
        BytesWrapper bw = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireTest$BytesWrapper {\n" +
                "  bytes: \"\"\n" +
                "}\n");

        // Modify the bytes content of the deserialized BytesWrapper.
        bw.bytes("");
        bw.bytes("hi");
        bw.bytes("hello");

        // Validate the string representation of the modified BytesWrapper.
        assertEquals("!net.openhft.chronicle.wire.TextWireTest$BytesWrapper {\n" +
                "  bytes: hello\n" +
                "}\n", bw.toString(), "bytes field should update correctly when modified after deserialization");

        // Release the last acquired bytes to prevent memory leaks.
        bw.bytes.releaseLast();
    }

    @Test
    public void testDoubleEngineering() {
        // Registering an alias 'D' for the DoubleWrapper class to shorten the serialized format.
        ClassAliasPool.CLASS_ALIASES.addAlias(DoubleWrapper.class, "D");

        // Test serialization of a DoubleWrapper object with the double values in "()"
        assertEquals("!D {\n" +
                "  d: 1.0,\n" +
                "  n: -1.0\n" +
                "}\n", new DoubleWrapper(1.0).toString(), "small double values should serialize with .0 suffix");
        assertEquals("!D {\n" +
                "  d: 11.0,\n" +
                "  n: -11.0\n" +
                "}\n", new DoubleWrapper(11.0).toString(), "two-digit double values should serialize with .0 suffix");
        assertEquals("!D {\n" +
                "  d: 101.0,\n" +
                "  n: -101.0\n" +
                "}\n", new DoubleWrapper(101.0).toString(), "three-digit double values should serialize with .0 suffix");
        assertEquals("!D {\n" +
                "  d: 1E3,\n" +
                "  n: -1E3\n" +
                "}\n", new DoubleWrapper(1e3)
                .toString(), "1e3 should serialize in engineering notation");

        // Test deserialization: Convert the string representation back to DoubleWrapper and verify its content.
        DoubleWrapper dw = Marshallable.fromString(new DoubleWrapper(1e3).toString());
        assertEquals(1e3, dw.d, 0, "1e3 should deserialize correctly from engineering notation");
        assertEquals("!D {\n" +
                "  d: 10E3,\n" +
                "  n: -10E3\n" +
                "}\n", new DoubleWrapper(10e3).toString(), "10e3 should serialize in engineering notation");
        DoubleWrapper dw2 = Marshallable.fromString(new DoubleWrapper(10e3).toString());
        assertEquals(10e3, dw2.d, 0, "10e3 should deserialize correctly from engineering notation");

        assertEquals("!D {\n" +
                "  d: 100E3,\n" +
                "  n: -100E3\n" +
                "}\n", new DoubleWrapper(100e3).toString(), "100e3 should serialize in engineering notation");
        DoubleWrapper dw3 = Marshallable.fromString(new DoubleWrapper(100e3).toString());
        assertEquals(100e3, dw3.d, 0, "100e3 should deserialize correctly from engineering notation");

        assertEquals("!D {\n" +
                "  d: 1E6,\n" +
                "  n: -1E6\n" +
                "}\n", new DoubleWrapper(1e6).toString(), "1e6 should serialize in engineering notation");
        DoubleWrapper dw4 = Marshallable.fromString(new DoubleWrapper(1e6).toString());
        assertEquals(1e6, dw4.d, 0, "1e6 should deserialize correctly from engineering notation");

        assertEquals("!D {\n" +
                "  d: 10E6,\n" +
                "  n: -10E6\n" +
                "}\n", new DoubleWrapper(10e6).toString(), "10e6 should serialize in engineering notation");
        DoubleWrapper dw5 = Marshallable.fromString(new DoubleWrapper(10e6).toString());
        assertEquals(10e6, dw5.d, 0, "10e6 should deserialize correctly from engineering notation");
    }

    // Tests the consistency of serialization and deserialization of NestedList objects and various property combinations.
    @Test
    public void testNestedList() {
        // Create a NestedList instance from its serialized string representation.
        NestedList nl = Marshallable.fromString("!" + NestedList.class.getName() + " {\n" +
                "  name: name,\n" +
                "  listA: [ { a: 1\n, b: 1.2 } ],\n" +
                "  listB: [ { a: 1 ,\nb: 1.2 }, { a: 3 , b: 2.3 } ]," +
                "  num: 128\n" +
                "}\n");

        // Define the expected serialized string format for the above NestedList.
        String expected = "!net.openhft.chronicle.wire.TextWireTest$NestedList {\n" +
                "  name: name,\n" +
                "  listA: [\n" +
                "    { a: 1, b: 1.2 }\n" +
                "  ],\n" +
                "  listB: [\n" +
                "    { a: 1, b: 1.2 },\n" +
                "    { a: 3, b: 2.3 }\n" +
                "  ],\n" +
                "  num: 128\n" +
                "}\n";

        // Check that the actual serialized string of the NestedList matches the expected format.
        assertEquals(expected, nl.toString(), "nested list should serialize with proper field ordering and formatting");

        // Test various permutations of the NestedList's properties.
        OUTER:
        for (int i = 0; i < 64; i++) {
            Set<Integer> set = new HashSet<>();

            StringBuilder cs = new StringBuilder(128).append("!net.openhft.chronicle.wire.TextWireTest$NestedList {\n");
            int z = i;
            for (int j = 0; j < 4; j++) {
                if (!set.add(z & 3))
                    continue OUTER;
                switch (z & 3) {
                    case 0:
                        cs.append("  name: name,\n");
                        break;

                    case 1:
                        cs.append("  listA: [\n")
                                .append("    { a: 1, b: 1.2 }\n")
                                .append("  ],\n");
                        break;

                    case 2:
                        cs.append("  listB: [\n")
                                .append("    { a: 1, b: 1.2 },\n")
                                .append("    { a: 3, b: 2.3 }\n")
                                .append("  ],\n");
                        break;

                    case 3:
                        cs.append("  num: 128,\n");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected selector");
                }
                z /= 4;
            }
            cs.append("}\n");
            NestedList nl2 = Marshallable.fromString(cs.toString());
            assertEquals(expected, nl2.toString(), "nested list should deserialize correctly regardless of field order");
        }
    }

    // Tests different array types using Wire that they are correctly identified and the values are correctly retrieved.
    @Test
    public void testArrayTypes() {
        // Create a Wire instance and append serialized array types and a text.
        Wire wire = createWire();
        wire.bytes().append("a: !type byte[], b: !type String[], c: hi");

        // Check that the deserialized type of "b" is String[].class.
        assertEquals(String[].class, wire.read("b").typeLiteral(), "String[] type literal should parse correctly");

        // Check that the deserialized type of "a" is byte[].class.
        assertEquals(byte[].class, wire.read("a").typeLiteral(), "byte[] type literal should parse correctly");

        // Check that the deserialized text of "c" is "hi".
        assertEquals("hi", wire.read("c").text(), "text value should read correctly after type literals");
    }

    @Test
    public void testArrayTypes1() {
        // Create a Wire instance and append data to its bytes
        Wire wire = createWire();
        wire.bytes().append("a: !type [B;, b: !type String[], c: hi");

        // Verify the data types and content retrieved from the wire
        assertEquals(String[].class, wire.read("b").typeLiteral(), "String[] type literal should parse correctly with [B notation");
        assertEquals(byte[].class, wire.read("a").typeLiteral(), "byte[] type literal should parse correctly as [B;");
        assertEquals("hi", wire.read("c").text(), "text value should read correctly after JVM-style type notation");
    }

    @Test
    public void testArrayTypes2() {
        // Iterate over a set of primitive class types
        for (Class<?> clz : new Class[]{byte.class, char.class, int.class, long.class, double.class, float.class, boolean.class}) {
            // Create a Wire instance and append data with the current class type to its bytes
            Wire wire = createWire();
            wire.bytes().append("a: [ !type ").append(clz.getName()).append("[] ], b: !type String[], c: hi");

            // Verify the data types and content retrieved from the wire for the current class type
            assertEquals(String[].class, wire.read("b").typeLiteral(), "String[] type literal should parse correctly for primitive array types");
            Collection<Class> classes = wire.read("a").typedMarshallable();
            assertArrayEquals(new Class[]{Array.newInstance(clz, 0).getClass()}, classes.toArray(), "primitive array type should parse correctly in collection");
            assertEquals("hi", wire.read("c").text(), "text value should read correctly after primitive array type literals");
        }
    }

    @Test
    public void readMarshallableAsEnum() {
        // Create a Wire instance and add alias for TWTSingleton class
        Wire wire = createWire();
        ClassAliasPool.CLASS_ALIASES.addAlias(TWTSingleton.class);
        wire.bytes().append("a: !TWTSingleton { },\n" +
                "b: !TWTSingleton {\n" +
                "}\n");

        // Verify that the wire contains instances of TWTSingleton
        assertEquals(TWTSingleton.INSTANCE, wire.read("a").object(), "singleton should deserialize to same instance with compact body");
        assertEquals(TWTSingleton.INSTANCE, wire.read("b").object(), "singleton should deserialize to same instance with multiline body");

    }

    @Test
    public void nestedWithEnumSet() {
        // Create a Wire instance and a NestedWithEnumSet object
        final Wire wire = createWire();
        NestedWithEnumSet n = new NestedWithEnumSet();
        n.list.add(new WithEnumSet("none"));
        n.list.add(new WithEnumSet("one", EnumSet.of(TimeUnit.DAYS)));
        n.list.add(new WithEnumSet("two", EnumSet.of(TimeUnit.DAYS, TimeUnit.HOURS)));

        // Write the NestedWithEnumSet object to the wire
        wire.write("hello")
                .object(NestedWithEnumSet.class, n);

        // Verify the content of the wire matches the NestedWithEnumSet object
        assertEquals("hello: {\n" +
                "  list: [\n" +
                "    { name: none },\n" +
                "    { name: one, timeUnits: [ DAYS ] },\n" +
                "    { name: two, timeUnits: [ HOURS, DAYS ] }\n" +
                "  ]\n" +
                "}\n", wire.toString(), "nested list with EnumSet should serialize with enum names");

        // Retrieve and verify the NestedWithEnumSet object from the wire
        NestedWithEnumSet a = wire.read("hello")
                .object(NestedWithEnumSet.class);
        assertEquals(n.toString(), a.toString(), "deserialized object should have same string representation as original");
        assertEquals(n, a, "nested list with EnumSet should roundtrip correctly");
    }

    @Test
    public void testParse2() {
        // Create and populate a MyDto object
        MyDto myDto1 = new MyDto();

        myDto1.strings.add("hello");
        myDto1.strings.add("world");

        // Convert the MyDto object to a string
        String cs = myDto1.toString();

        // Deserialize the string back into a MyDto object
        MyDto o = Marshallable.fromString(cs);

        // Verify the deserialized object matches the original MyDto object
        assertEquals(cs, o.toString(), "MyDto with string list should roundtrip through serialization");

        assert o.strings.size() == 2;
    }

    @Test
    public void longConverter() {
        // Create a TwoLongs instance with specified long values
        TwoLongs twoLongs = new TwoLongs(0x1234567890abcdefL, -1);

        // Verify the string representation of the TwoLongs instance
        assertEquals("!net.openhft.chronicle.wire.TextWireTest$TwoLongs {\n" +
                "  hexadecimal: 1234567890abcdef,\n" +
                "  hexa2: ffffffffffffffff\n" +
                "}\n", twoLongs.toString(), "long values with HexadecimalLongConverter should serialize as hex strings");

        // Ensure the string representation can be correctly deserialized back to the original object
        assertEquals(twoLongs, Marshallable.fromString(twoLongs.toString()), "hexadecimal long values should roundtrip correctly");
    }

    @Test
    public void testDoublePrecisionOverTextWire() {
        // Create a Wire instance and write a double value to it
        Wire wire = createWire();
        final double d = 0.000212345678901;
        wire.getValueOut().float64(d);

        // Create a TextWire instance from the original wire's bytes
        final TextWire textWire = TextWire.from(bytes.toString());
        final double d2 = textWire.getValueIn().float64();

        // Validate the double value remains consistent after the transfer
        Assertions.assertEquals(d, d2, 0, "double precision should be preserved through text wire serialization");
    }

    @Test
    public void testMapOfNamedKeys() {
        // Create a MapHolder and initialize its map with various implementations and contents
        MapHolder mh = new MapHolder();
        Map<RetentionPolicy, Double> map = Collections.singletonMap(RetentionPolicy.CLASS, 0.1);
        mh.map = new EnumMap<>(map);
        assertEquals("!net.openhft.chronicle.wire.TextWireTest$MapHolder {\n" +
                        "  map: {\n" +
                        "    CLASS: 0.1\n" +
                        "  }\n" +
                        "}\n",
                TEXT.asString(mh),
                "EnumMap should serialize with enum constant names as keys");
    }

    @Test
    public void testNullConsumedIssue269() {
        // Deserialize a FieldWithEnum instance from a string representation
        final FieldWithEnum fwe = Marshallable.fromString("!" + FieldWithEnum.class.getName() + " {" +
                "allowedFoos: !!null \"\",\n" +
                "orderLevel: CHILD\n" +
                "}");
        System.out.println("fwe = " + fwe);

        // Verify the deserialized FieldWithEnum object's properties
        assertNull(fwe.allowedFoos, "null field should deserialize as null when explicitly marked");
        assertEquals(OrderLevel.CHILD, fwe.orderLevel, "enum field should deserialize correctly after null field");
    }

    @Test
    public void commaInAValue() {
        // Append a string to a new Wire instance and read the value as an object
        String text = "[1,2,3]";
        Wire wire = createWire();
        wire.bytes().append(text);
        final Object list = wire.getValueIn().object();
        assertEquals("[1,2,3]", "" + list, "compact list notation should parse correctly");

        // Repeat the process with a slightly different input
        String text2 = "[ 1, 2, 3 ]";
        wire.bytes().clear().append(text2);
        final Object list2 = wire.getValueIn().object();
        assertEquals("[1, 2, 3]", "" + list2, "spaced list notation should parse correctly");
    }

    @Test
    public void commaInAValue2() {
        // Create a string with multiple data types (numbers and a string)
        String text = "[1,2,3,\"c\"]";

        // Initialize a wire and append the string to it
        Wire wire = createWire();
        wire.bytes().append(text);

        // Read the values from the wire as an object
        final Object list = wire.getValueIn().object();

        // Validate that the wire read the values correctly
        assertEquals("[1,2,3, c]", "" + list, "mixed list with numbers and strings should parse correctly");
    }

    @Test
    public void testDuration() {
        // Create a DurationHolder object with a set duration
        DurationHolder dh = new DurationHolder(1, Duration.ofSeconds(63));
        String h = dh.toString();

        // Print the string representation for debugging
        System.out.println(h);

        // Deserialize the string back into a DurationHolder object
        DurationHolder dh2 = Marshallable.fromString(h);

        // Check if the deserialized object matches the original
        assertEquals(dh, dh2, "Duration field should roundtrip correctly through serialization");
    }

    @Test
    public void readsComment() {
        Wire wire = createWire();
        String actual = WireCommentTestSupport.exerciseReadComments(wire);

        assertEquals("one\n" +
                "two\n" +
                "three\n" +
                "dto: !net.openhft.chronicle.wire.BinaryWireTest$DTO {\n" +
                "  text: text\n" +
                "}\n" +
                "\n", actual, "comments should be read and preserved in output");
    }

    @Test
    public void readMetaData() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        WireTestSupport.assertReadMetaData(wire);
    }

    @Test
    public void testNestedListInterleavedComments() {
        // Deserialize a string containing a nested list with interleaved comments to an object.
        YamlWireTest.StringArray obj = WireType.TEXT.fromString(YamlWireTest.StringArray.class,
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

        // Assert that the object was deserialized correctly without being affected by the comments.
        assertArrayEquals(new String[]{"bar", "quux"}, obj.strings, "nested list with interleaved comments should parse correctly");
    }

    @Test
    public void testListInterleavedComments() {
        // Deserialize a string containing a list with interleaved comments to an object.
        List<String> obj = Marshallable.fromString(
                "     # first\n" +
                        "[\n" +
                        "     # foo\n" +
                        "     'bar',\n" +
                        "     # baz\n" +
                        "     'quux'\n" +
                        "     # thud\n" +
                        "]\n" +
                        "     # fin\n");

        // Assert that the object was deserialised correctly without being affected by the comments.
        assertEquals(obj, Arrays.asList("bar", "quux"), "list with interleaved comments should parse correctly");
    }

    // Enum to demonstrate serialization of enum types
    public enum OrderLevel implements Marshallable {
        PARENT, CHILD
    }


    // Static class representing a Data Transfer Object (DTO)
    // with a 'Class' type field
    static class DTO extends SelfDescribingMarshallable {
        Class<?> type;
    }

    // Static class holding a Map with RetentionPolicy keys and Double values
    static class MapHolder extends SelfDescribingMarshallable {
        Map<RetentionPolicy, Double> map;
    }

    // Class representing a field having an Enum type and a byte array
    static final class FieldWithEnum extends SelfDescribingMarshallable {
        private final OrderLevel orderLevel = OrderLevel.PARENT;
        private byte[] allowedFoos;
    }

    // Class containing a field with an associated comment
    static class FieldWithComment extends SelfDescribingMarshallable {
        @Comment("a comment where the value=%s")
        String field;
        // String field2;
    }

    // Class containing two fields, one of which has an associated comment
    static class FieldWithComment2 extends SelfDescribingMarshallable {
        @Comment("a comment where the value=%s")
        String field;
        String field2;
    }

    // Class holding a string and two integer fields,
    // and a map to manage unexpected fields
    static class TwoFields extends AbstractMarshallableCfg {
        String b;
        int d;
        int notThere;
        // transient Map to hold other unexpected fields
        final transient Map<String, Object> others = new LinkedHashMap<>();

        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            others.put(event.toString(), valueIn.object());
        }
    }

    // Class with fields of Bytes type initialised with various Byte buffers.
    @SuppressWarnings("java:S116") // Keep A,B,C,D uppercase to match expected YAML keys in assertions
    static class ABCD extends SelfDescribingMarshallable implements Monitorable {
        final Bytes<?> A = Bytes.allocateElasticDirect();
        final Bytes<?> B = Bytes.allocateDirect(64);
        final Bytes<?> C = Bytes.allocateElasticOnHeap();
        final Bytes<?> D = Bytes.allocateElasticOnHeap(1);

        // Method to release all byte buffers
        void releaseAll() {
            A.releaseLast();
            B.releaseLast();
            C.releaseLast();
            D.releaseLast();
        }

        @Override
        public void unmonitor() {
            Monitorable.unmonitor(A);
            Monitorable.unmonitor(B);
            Monitorable.unmonitor(C);
            Monitorable.unmonitor(D);
        }
    }

    // Class containing three StringBuilder fields
    @SuppressWarnings("java:S116") // Keep A,B,C uppercase to match expected YAML keys in assertions
    static class ABC extends SelfDescribingMarshallable {
        StringBuilder A = new StringBuilder();
        StringBuilder B = new StringBuilder();
        StringBuilder C = new StringBuilder();
    }

    // Nested class having another nested class field and a long field
    private static class NestedA extends SelfDescribingMarshallable {
        NestedB b;
        long value;
    }

    // Nested class containing a double field
    static class NestedB extends SelfDescribingMarshallable {
        double field1;
    }

    // Class containing a String array field
    static class StringArray implements Marshallable {
        String[] strings;
    }

    // Class wrapping a Bytes field and providing a method to set it
    static class BytesWrapper extends SelfDescribingMarshallable {
        @NotNull
        final
        Bytes<?> bytes = allocateElasticDirect();

        void bytes(@NotNull CharSequence cs) {
            bytes.clear();
            bytes.append(cs);
        }
    }

    // Class wrapping two double fields with a constructor to set them
    static class DoubleWrapper extends SelfDescribingMarshallable {
        final double d;
        final double n;

        DoubleWrapper(double d) {
            this.d = d;
            this.n = -d;
        }
    }

    // Class representing a nested list structure, capable of marshallable reading.
    static class NestedList extends SelfDescribingMarshallable {
        String name;
        final List<NestedItem> listA = new ArrayList<>();
        final List<NestedItem> listB = new ArrayList<>();
        final transient List<NestedItem> listA2 = new ArrayList<>();
        final transient List<NestedItem> listB2 = new ArrayList<>();
        int num;

        // Override readMarshallable to define custom deserialization logic from a wire format.
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            // Assign various fields from the wire format
            name = wire.read("name").text();
            wire.read("listA").sequence(listA, listA2, NestedItem::new);
            wire.read("listB").sequence(listB, listB2, NestedItem::new);
            num = wire.read("num").int32();
        }
    }

    // Nested item class to be utilized within NestedList, holding integral and floating-point data.
    private static class NestedItem extends SelfDescribingMarshallable {
        int a;
        double b;
    }

    // Class encapsulating a list of WithEnumSet instances, providing a structure for nesting.
    static class NestedWithEnumSet extends SelfDescribingMarshallable {
        final List<WithEnumSet> list = new ArrayList<>();
    }

    // Class representing an item that pairs a name with a set of TimeUnit enumeration items.
    static class WithEnumSet extends SelfDescribingMarshallable {
        String name;
        Set<TimeUnit> timeUnits = EnumSet.noneOf(TimeUnit.class);

        // Default constructor, utilized via reflection
        @UsedViaReflection
        WithEnumSet() {
        }

        // Overloaded constructor to initialize name field.
        public WithEnumSet(String name) {
            this.name = name;
        }

        // Overloaded constructor to initialize both name and timeUnits fields.
        public WithEnumSet(String name, Set<TimeUnit> timeUnits) {
            this.name = name;
            this.timeUnits = timeUnits;
        }

        // Define how this object should be written out to the wire format.
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            Wires.writeMarshallable(this, wire, false);
        }
    }

    // Class holding a list of strings with customized marshallable reading.
    static class MyDto extends SelfDescribingMarshallable {
        final List<String> strings = new ArrayList<>();

        // Define a custom way to read objects of this type from the wire format.
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {

            // WORKS
            // Wires.readMarshallable(this, wire, true);  // WORKS

            // FAILS
            Wires.readMarshallable(this, wire, false);
        }
    }

    // Class holding byte storage and a long, with custom serialization logic.
    static class DtoWithBytesField extends SelfDescribingMarshallable {
        BytesStore<?, ?> bytes;
        long another;

        // Implement custom deserialization logic for this object.
        @Override
        public void readMarshallable(@NotNull WireIn wire) {
            // Initialize bytes field as a native pointer if null and read bytes and long data from the wire.
            if (bytes == null)
                bytes = BytesStore.nativePointer();
            wire.read(() -> "bytes").bytesSet((PointerBytesStore) bytes);
            another = (wire.read(() -> "another").int64());
        }

        // Implement custom serialization logic for this object.
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            // Write bytes and long data to the wire format.
            wire.write(() -> "bytes").bytes(bytes);
            wire.write(() -> "another").int64(another);
        }
    }

    // Class storing two long integers with hexadecimal conversion, designed for wire transport.
    static class TwoLongs extends SelfDescribingMarshallable {

        @LongConversion(HexadecimalLongConverter.class)
        final
        long hexadecimal;

        @LongConversion(HexadecimalLongConverter.class)
        final
        long hexa2;

        // Constructor initializing both long fields.
        TwoLongs(long hexadecimal, long hexa2) {
            this.hexadecimal = hexadecimal;
            this.hexa2 = hexa2;
        }
    }

    // Class encapsulating an integer and a Duration object, to be serialized/deserialized.
    static class DurationHolder extends SelfDescribingMarshallable {
        final int foo;
        final Duration duration;

        // Constructor initializing both fields.
        DurationHolder(int foo, Duration duration) {
            this.foo = foo;
            this.duration = duration;
        }
    }

    // Basic class capable of being serialized/deserialized without field definition.
    private static class Circle implements Marshallable {
    }
}
