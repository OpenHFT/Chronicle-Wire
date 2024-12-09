/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.Monitorable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
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
import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes", "unchecked", "try", "serial", "deprecation"})
public class TextWireTest extends WireTestCommon {

    // Create a new TextWire instance with an elastic heap allocated buffer
    static Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
    Bytes<?> bytes;

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
            assertEquals(Arrays.asList("a", "b", "c"), list);
        }
    }

    @Test
    public void testWhiteSpaceInType() {
        try {
            // Deserialize from string and check if the object is correctly formed
            Object o = Marshallable.fromString("key: !" + DTO.class.getName() + " {\n" +
                    "  type:            !type               String\n" +
                    "}\n");

            assertNotNull(o);

        } catch (Exception e) {
            Assert.fail();
        }
    }

    // Test handling of Bytes data type in TextWire.
    @Test
    public void testBytes() {
        @NotNull Wire wire = createWire();
        @NotNull byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;

        // Write different bytes sequences to the wire
        wire.write().bytes(NoBytesStore.NO_BYTES)
                .write().bytes(Bytes.wrapForRead("Hello".getBytes(ISO_8859_1)))
                .write().bytes(Bytes.wrapForRead("quotable, text".getBytes(ISO_8859_1)))
                .write().bytes(allBytes);
        // System.out.println(bytes.toString());

        // Read back the bytes sequences and validate their content
        @NotNull Bytes<?> allBytes2 = allocateElasticOnHeap();
        wire.read().bytes(b -> assertEquals(0, b.readRemaining()))
                .read().bytes(b -> assertEquals("Hello", b.toString()))
                .read().bytes(b -> assertEquals("quotable, text", b.toString()))
                .read().bytes(allBytes2);
        assertEquals(Bytes.wrapForRead(allBytes), allBytes2);
    }

    // Test handling of comments in TextWire.
    @Test
    public void comment() {
        @NotNull Wire wire = createWire();
        wire.writeComment("\thi: omg");
        wire.write("hi").text("there");
        assertEquals("there", wire.read("hi").text());
    }

    // Test handling of type specification instead of an actual field in TextWire.
    @Test
    public void testTypeInsteadOfField() {
        Wire wire = TextWire.from("!!null \"\"");
        StringBuilder sb = new StringBuilder();
        wire.read(sb).object(Object.class);
        assertEquals(0, sb.length());
    }

    // Test serialization with fields accompanied by comments in TextWire.
    @Test
    public void testFieldWithComment() {
        FieldWithComment f = new FieldWithComment();
        f.field = "hello world";
        Assert.assertEquals("!net.openhft.chronicle.wire.TextWireTest$FieldWithComment {\n" +
                "  field: hello world, \t\t# a comment where the value=hello world\n" +
                "}\n", Marshallable.$toString(f));
    }

    // Test serialization with multiple fields accompanied by comments in TextWire.
    @Test
    public void testFieldWithComment2() {
        FieldWithComment2 f = new FieldWithComment2();
        f.field = "hello world";
        Assert.assertEquals("!net.openhft.chronicle.wire.TextWireTest$FieldWithComment2 {\n" +
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

        assertEquals("ROUND_ROBIN", o.get("policy"));
        assertEquals(Collections.singletonList("INT1"), o.get("routes"));
        assertEquals("@Symbol =~ \"[A-L].*\"", o.get("pattern"));
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
                asProperties(tf.others));

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
                asProperties(tf2.others));

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
                asProperties(tf3.others));
    }

    // Utility method to convert a map to a string representation in properties format.
    public String asProperties(Map<String, Object> map) {
        return map.entrySet().stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    // Test to check the license validation for different WireTypes.
    @Test
    public void licenseCheck() {
        // Verify that TEXT WireType doesn't require any license check.
        WireType.TEXT.licenceCheck();
        assertTrue(WireType.TEXT.isAvailable());
    }

    // Test to ensure that objects with TreeMap fields are correctly serialized and deserialized.
    @Test
    public void writeObjectWithTreeMap() {
        @NotNull Wire wire = createWire();
        ObjectWithTreeMap value = new ObjectWithTreeMap();
        value.map.put("hello", "world");
        wire.write().object(value);

        // Uncomment the below line to see the serialized content of the wire.
        // System.out.println(wire);

        // Deserialize the wire content back to an object and check the content of the map.
        ObjectWithTreeMap value2 = new ObjectWithTreeMap();
        wire.read().object(value2, ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value2.map.toString());

        // Repeat the above steps with different ways of reading the wire content.
        wire.bytes().readPosition(0);
        ObjectWithTreeMap value3 = new ObjectWithTreeMap();
        wire.read().object(value3, Object.class);
        assertEquals("{hello=world}", value3.map.toString());

        wire.bytes().readPosition(0);
        ObjectWithTreeMap value4 = wire.read().object(ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value4.map.toString());
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
        Assert.assertTrue(w instanceof Map);
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
            assertEquals(i, w.read("data").int64());
            assertEquals(i, w.read("data2").int64());
        }
    }

    // Test to serialize a data structure in BINARY format and then try to convert it to TEXT format.
    @Test
    public void testWriteToBinaryAndTriesToConvertToText() {

        Bytes<?> b = Bytes.elasticByteBuffer();
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
        // System.out.println(textYaml);
        // Deserialize the TEXT into an object and verify its structure.
        @Nullable Object o = WireType.TEXT.fromString(textYaml);
        Assert.assertEquals("{map={some={key=value}, some-other={key=value}}}", o.toString());

        b.releaseLast();
    }

    // Test to ensure calling the 'write()' method multiple times will produce the expected string.
    @Test
    public void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        assertEquals("\"\": \"\": \"\": ", wire.toString());
    }

    // Utility method to reset and retrieve the wire and bytes objects.
    @NotNull
    private Wire createWire() {
        wire.reset();
        bytes = wire.bytes();
        return wire;
    }

    // Test to validate the behavior of writing and reading simple boolean values
    @Test
    public void testSimpleBool() {
        @NotNull Wire wire = createWire();

        // Write two boolean values with keys "F" and "T"
        wire.write(() -> "F").bool(false);
        wire.write(() -> "T").bool(true);

        // Verify that the written values are correctly represented in string format
        assertEquals("F: false\n" +
                "T: true\n", wire.toString());

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{F=false, T=true}";
        expectWithSnakeYaml(expected, wire);

        // Read and validate the written boolean values
        assertFalse(wire.read(() -> "F").bool());
        assertTrue(wire.read(() -> "T").bool());
    }

    // Test to validate the behavior when writing strings that cannot be converted to boolean
    @Test
    public void testFailingBool() {
        @NotNull Wire wire = createWire();

        // Write two non-boolean strings with keys "A" and "B"
        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");

        // Verify the written strings
        assertEquals("A: \"\"\n" +
                "B: other\n", wire.toString());

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        // Ensure the written strings are not mistakenly interpreted as boolean values
        assertFalse(wire.read(() -> "A").bool());
        assertFalse(wire.read(() -> "B").bool());
    }

    // Test to validate the reading of non-boolean strings as Boolean objects
    @Test
    public void testFailingBoolean() {
        @NotNull Wire wire = createWire();

        // Write two non-boolean strings
        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");

        // Verify the written strings
        assertEquals("A: \"\"\n" +
                "B: other\n", wire.toString());

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        // TODO: Handle the potential issue when reading a string as a Boolean object
        // assertEquals(null, wire.read(() -> "A").object(Boolean.class));
        assertEquals(false, wire.read(() -> "B").object(Boolean.class));
    }

    // Test to validate the behavior when writing text with a leading space
    @Test
    public void testLeadingSpace() {
        @NotNull Wire wire = createWire();

        // Write a string with a leading space
        wire.write().text(" leadingspace");

        // Ensure that the leading space is retained when reading back the string
        assertEquals(" leadingspace", wire.read().text());
    }

    // Helper method to validate the wire content using the SnakeYaml parser
    private void expectWithSnakeYaml(String expected, @NotNull Wire wire) {
        String s = wire.toString();
        @Nullable Object load = null;
        try {
            @NotNull Yaml yaml = new Yaml();
            load = yaml.load(new StringReader(s));
        } catch (Exception e) {
            throw e;
        }
        assertEquals(expected, load.toString());
    }

    // Test to validate the behavior of writing and reading a long value
    @Test
    public void testInt64() {
        @NotNull Wire wire = createWire();

        // Write a long value with the key "VALUE"
        long expected = 1234567890123456789L;
        wire.write(() -> "VALUE").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=1234567890123456789}", wire);

        // Read and validate the written long value
        assertEquals(expected, wire.read(() -> "VALUE").int64());
    }

    // Test to validate the behavior of writing and reading a short value
    @Test
    public void testInt16() {
        @NotNull Wire wire = createWire();

        // Write a short value with the key "VALUE"
        short expected = 12345;
        wire.write(() -> "VALUE").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=12345}", wire);

        // Read and validate the written short value
        assertEquals(expected, wire.read(() -> "VALUE").int16());
    }

    // Test to ensure that reading a value too large for a short throws an exception
    @Test(expected = IllegalStateException.class)
    public void testInt16TooLarge() {
        @NotNull Wire wire = createWire();

        // Write the maximum long value with the key "VALUE"
        wire.write(() -> "VALUE").int64(Long.MAX_VALUE);

        // Attempt to read the value as a short, which should throw an exception
        wire.read(() -> "VALUE").int16();
    }

    // Test to validate the behavior of writing and reading an integer value
    @Test
    public void testInt32() {
        @NotNull Wire wire = createWire();

        // Write an integer value with the keys "VALUE" and "VALUE2"
        int expected = 1;
        wire.write(() -> "VALUE").int64(expected);
        wire.write(() -> "VALUE2").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=1, VALUE2=1}", wire);

        // Read and validate the written integer values
        assertEquals(expected, wire.read(() -> "VALUE").int16());
        assertEquals(expected, wire.read(() -> "VALUE2").int16());
    }

    // Test to ensure that reading a value too large for an integer throws an exception
    @Test(expected = IllegalStateException.class)
    public void testInt32TooLarge() {
        @NotNull Wire wire = createWire();

        // Write the maximum integer value with the key "VALUE"
        wire.write(() -> "VALUE").int64(Integer.MAX_VALUE);

        // Attempt to read the value as a short, which should throw an exception
        wire.read(() -> "VALUE").int16();
    }

    // Test to validate writing using keys from the BWKey enum
    @Test
    public void testWrite1() {
        @NotNull Wire wire = createWire();

        // Write fields using BWKey enum values
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);

        // Verify the wire content
        assertEquals("field1: field2: field3: ", wire.toString());
    }

    // Test to validate writing with different string lengths
    @Test
    public void testWrite2() {
        @NotNull Wire wire = createWire();

        // Write strings with varying lengths
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");

        // Verify the wire content
        assertEquals("Hello: World: \"Long field name which is more than 32 characters, Bye\": ", wire.toString());
    }

    // Test to validate reading from the wire
    @Test
    public void testRead() {
        @NotNull Wire wire = createWire();

        // Write values to the wire
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");

        // Read values from the wire
        wire.read();
        wire.read();
        wire.read();

        // Verify the remaining bytes after reading
        assertEquals(1, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test to validate reading using specific keys from the wire
    @Test
    public void testRead1() {
        @NotNull Wire wire = createWire();

        // Write values to the wire
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");

        // Read values using specific key. If the key is blank, it matches any key.
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // This read should not match the previous write key, but the test doesn't assert any behavior.
        wire.read(BWKey.field1);

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
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
        assertEquals(0, name.length());

        // Reading the next two values into StringBuilder and verifying their contents
        wire.read(name);
        assertEquals(BWKey.field1.name(), name.toString());

        wire.read(name);
        assertEquals(name1, name.toString());

        // Verify the remaining bytes after reading
        assertEquals(1, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test to validate writing and reading 8-bit integers from the wire
    @Test
    public void int8() {
        @NotNull Wire wire = createWire();

        // Write 8-bit integers to the wire with various keys
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);
        wire.write(() -> "Test").int8(3);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // Read the 8-bit integers from the wire and validate their values
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int8(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test case to validate writing and reading 16-bit integers from the wire
    @Test
    public void int16() {
        @NotNull Wire wire = createWire();

        // Write 16-bit integers to the wire with various keys
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);
        wire.write(() -> "Test").int16(3);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // Read the 16-bit integers from the wire and validate their values
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test case to validate writing and reading unsigned 8-bit integers from the wire
    @Test
    public void uint8() {
        @NotNull Wire wire = createWire();

        // Write unsigned 8-bit integers to the wire with various keys
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write(() -> "Test").uint8(3);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // Read the unsigned 8-bit integers from the wire and validate their values
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint8(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test case to validate writing and reading unsigned 16-bit integers from the wire
    @Test
    public void uint16() {
        @NotNull Wire wire = createWire();

        // Write unsigned 16-bit integers to the wire with various keys
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write(() -> "Test").uint16(3);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // Read the unsigned 16-bit integers from the wire and validate their values
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test case to validate writing and reading unsigned 32-bit integers from the wire
    @Test
    public void uint32() {
        @NotNull Wire wire = createWire();

        // Write unsigned 32-bit integers to the wire with various keys
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write(() -> "Test").uint32(3);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // Read the unsigned 32-bit integers from the wire and validate their values
        @NotNull AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test case to validate writing and reading 32-bit integers from the wire
    @Test
    public void int32() {
        @NotNull Wire wire = createWire();

        // Write 32-bit integers to the wire with various keys
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write(() -> "Test").int32(3);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // Read the 32-bit integers from the wire and validate their values
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test case to validate writing and reading 64-bit integers from the wire
    @Test
    public void int64() {
        @NotNull Wire wire = createWire();

        // Write 64-bit integers to the wire with various keys
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);

        // Validate wire contents using the SnakeYaml parser and the expected string format
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // Read the 64-bit integers from the wire and validate their values
        @NotNull AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        // Verify the remaining bytes after reading
        assertEquals(0, bytes.readRemaining());

        // Check that it's safe to read more than the remaining bytes
        wire.read();
    }

    // Test case for writing and reading 64-bit floating point numbers
    @Test
    public void float64() {
        @NotNull Wire wire = createWire();

        // Write 64-bit floating point numbers to the wire with various keys
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write(() -> "Test").float64(3);

        // Validate the wire's string format
        assertEquals("\"\": 1.0\n" +
                "field1: 2.0\n" +
                "Test: 3.0\n", wire.toString());

        // Validate using SnakeYAML parser
        expectWithSnakeYaml("{=1.0, field1=2.0, Test=3.0}", wire);

        // Read values using a custom float wrapper and validate
        class Floater {
            double f;

            public void set(double d) {
                f = d;
            }
        }

        @NotNull Floater n = new Floater();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n, Floater::set);
            assertEquals(e, n.f, 0.0);
        });

        assertEquals(0, bytes.readRemaining());
        wire.read();  // Extra read to check safety
    }

    // Test case for writing and reading text values
    @Test
    public void text() {
        @NotNull Wire wire = createWire();

        // Write text values to the wire
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        @NotNull String name = "Long field name which is more than 32 characters, \\ \nBye";

        wire.write(() -> "Test")
                .text(name);
        // Validate using SnakeYAML parser and the expected string format
        expectWithSnakeYaml("{=Hello, field1=world, Test=Long field name which is more than 32 characters, \\ \n" +
                "Bye}", wire);
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, \\\\ \\nBye\"\n", wire.toString());

        // Read the texts back and validate
        @NotNull StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name).forEach(e -> {
            assertNotNull(wire.read().textTo(sb));
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.readRemaining());
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
                "Test: !" + name1 + " # \n", wire.toString());

        // Read the types back and validate
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().typePrefix(e, Assert::assertEquals);
        });

        assertEquals(0, bytes.readRemaining());
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
                "}\n", a.toString());
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

        assertNotNull(a);  // Check that the resulting object is not null
    }

    // Test case for writing and reading boolean values
    @Test
    public void testBool() {
        @NotNull Wire wire = createWire();

        // Write boolean values to the wire
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);

        // Read back the boolean values and validate
        wire.read().bool(false, Assert::assertEquals)
                .read().bool(true, Assert::assertEquals)
                .read().bool(null, Assert::assertEquals);
    }

    // Test case for writing and reading 32-bit floating point numbers
    @Test
    public void testFloat32() {
        @NotNull Wire wire = createWire();

        // Write float values to the wire
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY)
                .write().float32(Float.NEGATIVE_INFINITY)
                .write().float32(123456.0f);

        // Read back the float values and validate
        wire.read().float32(this, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(this, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(this, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(Float.NEGATIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(123456.0f, t, 0.0F));
    }

    // Test case for writing and reading LocalTime values
    @Test
    public void testTime() {
        @NotNull Wire wire = createWire();

        LocalTime now = LocalTime.now();

        // Write LocalTime values to the wire
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);

        // Validate the string format of the wire content
        assertEquals("\"\": " + now + "\n" +
                        "\"\": 23:59:59.999999999\n" +
                        "\"\": 00:00\n",
                bytes.toString());

        // Read back the LocalTime values and validate
        wire.read().time(now, Assert::assertEquals)
                .read().time(LocalTime.MAX, Assert::assertEquals)
                .read().time(LocalTime.MIN, Assert::assertEquals);
    }

    // Test case for working with ZonedDateTime values
    @Test
    public void testZonedDateTime() {
        @NotNull Wire wire = createWire();

        // Create several ZonedDateTime instances: now, max, min
        ZonedDateTime now = ZonedDateTime.now();
        ZoneId zone = ZoneId.of("Europe/London");
        final ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, zone);
        final ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, zone);

        // Write the ZonedDateTime values to the wire
        wire.write()
                .zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);

        // Validate the string representation of the wire content
        assertEquals("\"\": \"" + now + "\"\n" +
                "\"\": \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                "\"\": \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString());

        // Read back the ZonedDateTime values and validate
        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(max, Assert::assertEquals)
                .read().zonedDateTime(min, Assert::assertEquals);

        // Repeat the process but write as a generic object
        wire.clear();
        wire.write().object(now)
                .write().object(max)
                .write().object(min);
        assertEquals("\"\": !ZonedDateTime \"" + now + "\"\n" +
                "\"\": !ZonedDateTime \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                "\"\": !ZonedDateTime \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString());
        wire.read().object(Object.class, now, Assert::assertEquals)
                .read().object(Object.class, max, Assert::assertEquals)
                .read().object(Object.class, min, Assert::assertEquals);

        // Write as a ZonedDateTime object
        wire.clear();
        wire.write().object(ZonedDateTime.class, now)
                .write().object(ZonedDateTime.class, max)
                .write().object(ZonedDateTime.class, min);
        assertEquals("\"\": \"" + now + "\"\n" +
                "\"\": \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                "\"\": \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString());
        wire.read().object(ZonedDateTime.class, now, Assert::assertEquals)
                .read().object(ZonedDateTime.class, max, Assert::assertEquals)
                .read().object(ZonedDateTime.class, min, Assert::assertEquals);
    }

    // Test case for working with LocalDate values
    @Test
    public void testDate() {
        @NotNull Wire wire = createWire();

        // Create LocalDate instance: now
        LocalDate now = LocalDate.now();

        // Write the LocalDate values to the wire
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);

        // Read back the LocalDate values and validate
        wire.read().date(now, Assert::assertEquals)
                .read().date(LocalDate.MAX, Assert::assertEquals)
                .read().date(LocalDate.MIN, Assert::assertEquals);
    }

    // Test case for working with UUID values
    @Test
    public void testUuid() {
        @NotNull Wire wire = createWire();

        // Generate a random UUID
        UUID uuid = UUID.randomUUID();

        // Write the UUID values to the wire
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));

        // Read back the UUID values and validate
        wire.read().uuid(uuid, Assert::assertEquals)
                .read().uuid(new UUID(0, 0), Assert::assertEquals)
                .read().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), Assert::assertEquals);
    }

    @Test
    public void testTypeWithoutSpace() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Append a type information to the wire (in a specific format)
        wire.bytes().append("A: !").append(MyTypes.class.getName()).append("{}");

        // Read from the wire and cast it to MyTypes
        @NotNull MyTypes mt = (MyTypes) wire.read(() -> "A").object();

        // Validate the read object's content using its toString() method
        assertEquals("!net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: \"\",\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n", mt.toString());
    }

    @Test
    public void testNANValue() {
        // Create a wire instance and append special floating-point values
        @NotNull Wire wire = createWire();
        wire.bytes().append(
                "A: NaN,\n" +
                        "A2: NaN ,\n" +
                        "A3: Infinity,\n" +
                        "A4: -Infinity,\n" +
                        "A5: NaN\n" +
                        "B: 1.23\n");

        // Validate reading back the values from the wire
        assertEquals(Double.NaN, wire.read("A").float64(), 0);
        assertEquals(Double.NaN, wire.read("A2").float64(), 0);
        assertEquals(Double.POSITIVE_INFINITY, wire.read("A3").float64(), 0);
        assertEquals(Double.NEGATIVE_INFINITY, wire.read("A4").float64(), 0);
        assertEquals(Double.NaN, wire.read("A5").float64(), 0);
        assertEquals(1.23, wire.read("B").float64(), 0);
    }

    @Test
    public void testABCDBytes() {
        // Create a wire instance and append data with different byte representations
        @NotNull Wire wire = createWire();
        wire.bytes().append(
                "A: \"hi\",\n" +
                        "B: 'hi',\n" +
                        "C: hi,\n" +
                        "D: bye,\n");
        ABCD abcd = new ABCD();

        try {
            for (int i = 0; i < 5; i++) {
                // Reset read position of the wire for each iteration
                wire.bytes().readPosition(0);

                // Validate reading back the values from the wire
                assertEquals("!net.openhft.chronicle.wire.TextWireTest$ABCD {\n" +
                        "  A: hi,\n" +
                        "  B: hi,\n" +
                        "  C: hi,\n" +
                        "  D: bye\n" +
                        "}\n", wire.getValueIn()
                        .object(abcd, ABCD.class)
                        .toString());
            }
        } finally {
            // Release resources associated with the ABCD instance
            abcd.releaseAll();

            // Get default value of ABCD and release its resources
            WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(ABCD.class);
            ABCD abcd0 = (ABCD) wm.defaultValue();
            abcd0.releaseAll();
        }
    }

    // Test the string building behavior for ABC objects with Wire.
    @Test
    public void testABCStringBuilder() {
        String A = "A: \"hi\", # This is an A\n";
        String B = "B: 'hi', # This is a B\n";
        String C = "C: hi, # And that's a C\n";

        // Create a wire and append values for A, B, and C
        @NotNull Wire wire = createWire();
        StringBuilder sb = new StringBuilder();
        wire.commentListener(s -> sb.append(s).append('\n'));
        ABC abc = new ABC();

        // Read from wire and assert its value for all permutations
        for (String input : new String[] { A + B + C, B + A + C, C + A + B, A + C + B, B + C + A, C + B + A }) {
            wire.reset();
            wire.bytes().append(input);
            assertEquals(input, "!net.openhft.chronicle.wire.TextWireTest$ABC {\n" +
                    "  A: hi,\n" +
                    "  B: hi,\n" +
                    "  C: hi\n" +
                    "}\n", wire.getValueIn()
                    .object(abc, ABC.class)
                    .toString());
            assertEquals(sb.toString(),
                    // legacy behavior: "C" comment is ignored as it's after the last field
                    Arrays.asList("This is an A", "This is a B"),
                    Arrays.stream(sb.toString().split("\n")).sorted(Collections.reverseOrder()).collect(toList()));
            sb.setLength(0);
        }
    }

    // Test reading and writing of a string map with Wire.
    @Test
    public void testMapReadAndWriteStrings() {
        // Initialize bytes and wire for writing
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = WireType.TEXT.apply(bytes);

        // Define the expected map
        @NotNull final Map<String, String> expected = new LinkedHashMap<>();

        expected.put("hello", "world");
        expected.put("hello1", "world1");
        expected.put("hello2", "world2");

        // Write the map to wire
        wire.writeDocument(false, o -> {
            o.writeEventName(() -> "example")
                    .map(expected);
        });
        // bytes.readPosition(4);
        // expectWithSnakeYaml("{example={hello=world, hello1=world1, hello2=world2}}", wire);
        // bytes.readPosition(0);

        // Assert the written map's format
        assertEquals("--- !!data\n" +
                        "example: {\n" +
                        "  hello: world,\n" +
                        "  hello1: world1,\n" +
                        "  hello2: world2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes));

        // Read the map from wire and assert equality with the original
        @NotNull final Map<String, String> actual = new LinkedHashMap<>();
        wire.readDocument(null, c -> c.read(() -> "example").marshallableAsMap(String.class, String.class, actual));
        assertEquals(expected, actual);
    }

    // Test behavior when using fields of type Bytes.
    // Note: This test is ignored due to unreleased bytes.
    @Test
    @Ignore("unreleased bytes")
    public void testBytesField() {
        DtoWithBytesField dto = new DtoWithBytesField(), dto2 = null;
        byte[] binaryData = new byte[]{1, 2, 3, 4};
        dto.bytes = Bytes.wrapForRead(binaryData);
        dto.another = 123L;

        try {
            // Convert the DTO to string and back, and assert equality
            String cs = dto.toString();
            dto2 = Marshallable.fromString(cs);
            assertEquals(cs, dto2.toString());
        } finally {
            // Ensure resources are cleaned up
            dto.bytes.releaseLast();
            dto2.bytes.releaseLast();
        }
    }

    // Test the write behavior of custom Marshallable objects with Wire.
    @Test
    public void testWriteMarshallable() {
        // Create wire instance
        @NotNull Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;
        mtA.text.append("Hello World");

        // Write the first Marshallable instance (mtA) to wire
        wire.write(() -> "A").marshallable(mtA);

        @NotNull MyTypesCustom mtB = new MyTypesCustom();
        mtB.flag = false;
        mtB.d = 123.4567;
        mtB.i = -123457890;
        mtB.s = (short) 1234;
        mtB.text.append("Bye now");

        // Write the second Marshallable instance (mtB) to wire
        wire.write(() -> "B").marshallable(mtB);

        // Assert the string format of wire after writing
        assertEquals("A: {\n" +
                "  B_FLAG: true,\n" +
                "  S_NUM: 12345,\n" +
                "  D_NUM: 123.456,\n" +
                "  L_NUM: 0,\n" +
                "  I_NUM: -12345789,\n" +
                "  TEXT: Hello World\n" +
                "}\n" +
                "B: {\n" +
                "  B_FLAG: false,\n" +
                "  S_NUM: 1234,\n" +
                "  D_NUM: 123.4567,\n" +
                "  L_NUM: 0,\n" +
                "  I_NUM: -123457890,\n" +
                "  TEXT: Bye now\n" +
                "}\n", wire.bytes().toString());
        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=Hello World}, " +
                "B={B_FLAG=false, S_NUM=1234, D_NUM=123.4567, L_NUM=0, I_NUM=-123457890, TEXT=Bye now}}", wire);

        @NotNull MyTypesCustom mt2 = new MyTypesCustom();

        // Read the Marshallable instances from wire and assert equality
        wire.read(() -> "A").marshallable(mt2);
        assertEquals(mt2, mtA);

        wire.read(() -> "B").marshallable(mt2);
        assertEquals(mt2, mtB);
    }

    // Test the write behavior of custom Marshallable objects with Wire,
    // and verify the length of written fields.
    @Test
    public void testWriteMarshallableAndFieldLength() {
        // Create wire instance
        @NotNull Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;

        @NotNull ValueOut write = wire.write(() -> "A");

        // Determine the start position for field length calculation
        long start = wire.bytes().writePosition() + 1; // including one space for "sep".

        // Write the Marshallable instance to wire
        write.marshallable(mtA);

        // Calculate the length of written field
        long fieldLen = wire.bytes().lengthWritten(start);

        // Assert the string format of wire after writing
        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=}}", wire);

        @NotNull ValueIn read = wire.read(() -> "A");

        // Determine the length of the read field
        long len = read.readLength();

        // Assert the equality of calculated field lengths
        assertEquals(fieldLen, len, 1);
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
        wire.writeDocument(false, o -> {
            o.write(() -> "example").map(expected);
        });

        // Assert that the wire content matches expected format
        assertEquals("--- !!data\n" +
                "example: {\n" +
                "  ? !int 1: !int 11,\n" +
                "  ? !int 2: !int 2,\n" +
                "  ? !int 3: !int 3\n" +
                "}\n", Wires.fromSizePrefixedBlobs(bytes));

        // Read the map from wire and assert it matches the expected map
        @NotNull final Map<Integer, Integer> actual = new HashMap<>();
        wire.readDocument(null, c -> {
            @Nullable Map m = c.read(() -> "example").marshallableAsMap(Integer.class, Integer.class, actual);
            assertEquals(m, expected);
        });
    }

    // Test parsing a map within a map from a string
    @Test
    public void testMapInMap() {
        String pos = "WithMap: {\n" +
                "  innerMap: {\n" +
                "    AUDUSD: AUDUSD1,\n" +
                "    USDPLN: USDPLN1\n" +
                "  },\n" +
                "}";
        Map<String, Object> fromString = Marshallable.fromString(pos);
        assertEquals("{WithMap={innerMap={AUDUSD=AUDUSD1, USDPLN=USDPLN1}}}",
                fromString.toString());
    }

    // Test parsing a map with question marks (indicating explicit keys) within another map from a string
    @Test
    public void testMapInMapWithQuestionMarks() {
        String pos = "WithMap: {\n" +
                "  innerMap: {\n" +
                "    ? AUDUSD: AUDUSD1,\n" +
                "    ? USDPLN: USDPLN1\n" +
                "  },\n" +
                "}";
        Map<String, Object> fromString = Marshallable.fromString(pos);
        assertEquals("{WithMap={innerMap={AUDUSD=AUDUSD1, USDPLN=USDPLN1}}}",
                fromString.toString());
    }

    // Test reading and writing a map with Marshallable keys and values to/from a Wire.
    @Test
    public void testMapReadAndWriteMarshable() {
        // Create a byte store and wire to work with
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = WireType.TEXT.apply(bytes);

        // Populate the expected map with custom Marshallable objects
        @NotNull final Map<MyMarshallable, MyMarshallable> expected = new LinkedHashMap<>();

        expected.put(new MyMarshallable("aKey"), new MyMarshallable("aValue"));
        expected.put(new MyMarshallable("aKey2"), new MyMarshallable("aValue2"));

        // Write the map to wire
        wire.writeDocument(false, o -> o.write(() -> "example").marshallable(expected, MyMarshallable.class, MyMarshallable.class, true));

        // Assert that the wire content matches expected format
        assertEquals("--- !!data\n" +
                        "example: {\n" +
                        "  ? { MyField: aKey }: { MyField: aValue },\n" +
                        "  ? { MyField: aKey2 }: { MyField: aValue2 }\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes));

        // Read the map from wire and assert it matches the expected map
        @NotNull final Map<MyMarshallable, MyMarshallable> actual = new LinkedHashMap<>();

        wire.readDocument(null, c -> c.read(() -> "example")
                .marshallableAsMap(
                        MyMarshallable.class,
                        MyMarshallable.class,
                        actual));

        assertEquals(expected, actual);

        // Release any resources held by the byte store
        wire.bytes().releaseLast();
    }

    // Test writing an exception to a Wire and then reading it back.
    @Test
    public void testException() {
        // Create a custom exception with a mock stack trace
        @NotNull Exception e = new InvalidAlgorithmParameterException("Reference cannot be null") {
            @NotNull
            @Override
            public StackTraceElement[] getStackTrace() {
                @NotNull StackTraceElement[] stack = {
                        new StackTraceElement("net.openhft.chronicle.wire.TextWireTest", "testException", "TextWireTest.java", 783),
                        new StackTraceElement("net.openhft.chronicle.wire.TextWireTest", "runTestException", "TextWireTest.java", 73),
                        new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke0", "NativeMethodAccessorImpl.java", -2)
                };
                return stack;
            }
        };

        // Allocate a byte buffer and create a TEXT wire
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = WireType.TEXT.apply(bytes);
        wire.writeDocument(false, w -> w.writeEventName(() -> "exception")
                .object(e));

        assertEquals("--- !!data\n" +
                "exception: !" + e.getClass().getName() + " {\n" +
                "  message: Reference cannot be null,\n" +
                "  stackTrace: [\n" +
                "    { class: net.openhft.chronicle.wire.TextWireTest, method: testException, file: TextWireTest.java, line: 783 },\n" +
                "    { class: net.openhft.chronicle.wire.TextWireTest, method: runTestException, file: TextWireTest.java, line: 73 },\n" +
                "    { class: sun.reflect.NativeMethodAccessorImpl, method: invoke0, file: NativeMethodAccessorImpl.java, line: -2 }\n" +
                "  ]\n" +
                "}\n", Wires.fromSizePrefixedBlobs(bytes));

        // Read the exception from the wire and assert its type
        wire.readDocument(null, r -> {
            Throwable t = r.read(() -> "exception").throwable(true);
            assertTrue(t instanceof InvalidAlgorithmParameterException);
        });
    }

    // Test writing an enum to a Wire and then reading it back.
    @Test
    public void testEnum() {
        // Register an alias for the WireType enum
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class, "WireType");

        // Create a wire and write several enum values to it
        @NotNull Wire wire = createWire();
        wire.write().object(WireType.BINARY)
                .write().object(TEXT)
                .write().object(WireType.RAW);

        // Assert the wire's content matches the expected format
        assertEquals("\"\": !WireType BINARY\n" +
                "\"\": !WireType TEXT\n" +
                "\"\": !WireType RAW\n", bytes.toString());

        // Read the enum values from the wire and assert they match the original values
        assertEquals(WireType.BINARY, wire.read().object(Object.class));
        assertEquals(TEXT, wire.read().object(Object.class));
        assertEquals(WireType.RAW, wire.read().object(Object.class));
    }

    // Test writing arrays of objects to a Wire and reading them back.
    @Test
    public void testArrays() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Write an empty array to the wire
        @NotNull Object[] noObjects = {};
        wire.write("a").object(noObjects);

        // Assert that the wire represents the empty array correctly
        assertEquals("a: []\n", wire.toString());

        // Read the empty array from the wire and assert it's empty
        @Nullable Object[] object = wire.read().object(Object[].class);
        assertEquals(0, object.length);

        wire.clear();

        // Write an array of three strings to the wire
        @NotNull Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write("b").object(threeObjects);

        // Assert that the wire represents the three strings correctly
        assertEquals("b: [\n" +
                "  abc,\n" +
                "  def,\n" +
                "  ghi\n" +
                "]\n", wire.toString());

        // Read the array from the wire and assert its contents
        @Nullable Object[] object2 = wire.read()
                .object(Object[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    // Test writing arrays with varying lengths and types of elements to a Wire and reading them back.
    @Test
    public void testArrays2() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Write three arrays of different contents to the wire
        @NotNull Object[] a1 = new Object[0];
        wire.write("empty").object(a1);
        @NotNull Object[] a2 = {1L};
        wire.write("one").object(a2);
        @NotNull Object[] a3 = {"Hello", 123, 10.1};
        wire.write("three").object(Object[].class, a3);

        // Read the arrays from the wire and assert their contents
        @Nullable Object o1 = wire.read()
                .object(Object[].class);
        assertArrayEquals(a1, (Object[]) o1);
        @Nullable Object o2 = wire.read().object(Object[].class);
        assertArrayEquals(a2, (Object[]) o2);
        @Nullable Object o3 = wire.read().object(Object[].class);
        assertArrayEquals(a3, (Object[]) o3);
    }

    // Test GZIP compression of text strings written to a Wire.
    @Test
    public void testGZIPCompressionAsText() {
        // Create a wire instance and a string to compress
        @NotNull Wire wire = createWire();
        @NotNull final String s = "xxxxxxxxxxx1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        @NotNull String str = s + s + s + s;

        // Get the string as bytes and write it to the wire with gzip compression
        @NotNull byte[] compressedBytes = str.getBytes(ISO_8859_1);
        wire.write().compress("gzip", Bytes.wrapForRead(compressedBytes));

        // Read the compressed string from the wire, decompress it, and assert its content
        @NotNull Bytes<?> bytes = allocateElasticOnHeap();
        wire.read().bytes(bytes);
        assertEquals(str, bytes.toString());
    }

    // Test LZW compression of text strings written to a Wire.
    @Test
    public void testLZWCompressionAsText() {
        // Create a wire instance and a string to compress
        @NotNull Wire wire = createWire();
        @NotNull final String s = "xxxxxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxx";
        @NotNull String str = s + s + s + s;

        // Get the string as bytes and write it to the wire with LZW compression
        @NotNull byte[] compressedBytes = str.getBytes(ISO_8859_1);
        wire.write().compress("lzw", Bytes.wrapForRead(compressedBytes));

        // Read the compressed string from the wire, decompress it, and assert its content
        @NotNull Bytes<?> bytes = allocateElasticOnHeap();
        wire.read().bytes(bytes);
        assertEquals(str, bytes.toString());
    }

    // Test writing arrays of strings to a Wire and reading them back.
    @Test
    public void testStringArrays() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Write an empty string array to the wire
        @NotNull String[] noObjects = {};
        wire.write().object(noObjects);

        // Read the array back from the wire and ensure it's empty
        @Nullable String[] object = wire.read().object(String[].class);
        assertEquals(0, object.length);

        // Re-initialize the wire (the TODO suggests this step shouldn't be necessary)
        wire = createWire();

        // Write an array of three strings to the wire
        @NotNull String[] threeObjects = {"abc", "def", "ghi"};
        wire.write().object(threeObjects);

        // Read the array from the wire and verify its contents
        @Nullable String[] object2 = wire.read()
                .object(String[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    // Test writing lists of strings to a Wire and reading them back.
    @Test
    public void testStringList() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Write an empty string list to the wire
        @NotNull List<String> noObjects = new ArrayList();
        wire.write().object(noObjects);

        // Read the list from the wire and ensure it's empty
        @Nullable List<String> list = wire.read().object(List.class);
        assertEquals(0, list.size());

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        // Write a list of three strings to the wire
        @NotNull List<String> threeObjects = Arrays.asList("abc", "def", "ghi");
        wire.write().object(threeObjects);

        // Read the list from the wire and verify its contents
        @Nullable List<String> list2 = wire.read()
                .object(List.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }

    // Test writing sets of strings to a Wire and reading them back.
    @Test
    public void testStringSet() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Write an empty string set to the wire
        @NotNull Set<String> noObjects = new HashSet();
        wire.write().object(noObjects);

        // Read the set from the wire and ensure it's empty
        @Nullable Set<String> list = wire.read().object(Set.class);
        assertEquals(0, list.size());

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        // Write a set of three strings to the wire
        @NotNull Set<String> threeObjects = new HashSet(Arrays.asList("abc", "def", "ghi"));
        wire.write().object(threeObjects);

        // Read the set from the wire and verify its contents
        @Nullable Set<String> list2 = wire.read()
                .object(Set.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }

    // This test is for writing a Map<String, String> to the Wire and reading it back.
    // Currently, it's marked as ignored using the @Ignore annotation.
    @Test
    @Ignore
    public void testStringMap() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        // Create an empty map and write it to the wire
        @NotNull Map<String, String> noObjects = new HashMap();
        wire.write().object(noObjects);

        // Read the map from the wire and ensure it's empty
        @Nullable Map<String, String> map = wire.read().object(Map.class);
        assertEquals(0, map.size());

        // TODO we shouldn't need to create a new wire.
        // wire = createWire();
//
        // Set<String> threeObjects = new HashSet(Arrays.asList(new String[]{"abc", "def", "ghi"}));
        // wire.write().object(threeObjects);
//
        // Set<String> list2 = wire.read()
        // .object(Set.class);
        // assertEquals(3, list2.size());
        // assertEquals("[abc, def, ghi]", list2.toString());
    }

    // This test case demonstrates how to decode nested structures from a textual representation.
    @Test
    public void testNestedDecode() {
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
        // Create a wire instance
        @NotNull Wire wire = createWire();
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        // Read the objects back from the wire and ensure they are null
        @Nullable Object o = wire.read().object(Object.class);
        assertNull(o);
        @Nullable String s = wire.read().object(String.class);
        assertNull(s);
        @Nullable RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        assertNull(rp);
        @Nullable Circle c = wire.read().object(Circle.class);
        assertNull(c);
    }

    // Test to ensure all characters within the defined range are correctly written and read from a Wire
    @Test
    public void testAllChars() {
        @NotNull Wire wire = createWire();
        @NotNull char[] chars = new char[256];
        for (int i = 0; i < 1024; i++) {
            wire.clear();
            Arrays.fill(chars, (char) i);
            @NotNull String s = new String(chars);
            wire.writeDocument(false, w -> w.write(() -> "message").text(s));

            wire.readDocument(null, w -> w.read(() -> "message").text(s, Assert::assertEquals));
        }
    }

    // Test reading of a demarshallable object from the Wire and ensuring its integrity
    @Test
    public void readDemarshallable() {
        @NotNull Wire wire = createWire();
        try (DocumentContext $ = wire.writingDocument(true)) {
            wire.getValueOut().typedMarshallable(new DemarshallableObject("test", 12345));
        }

        assertEquals("40000052", Integer.toUnsignedString(wire.bytes().readInt(0), 16));
        assertEquals("!net.openhft.chronicle.wire.DemarshallableObject {\n" +
                "  name: test,\n" +
                "  value: 12345\n" +
                "}\n", wire.toString().substring(4));

        assertEquals("--- !!meta-data\n" +
                "!net.openhft.chronicle.wire.DemarshallableObject {\n" +
                "  name: test,\n" +
                "  value: 12345\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));

        try (DocumentContext $ = wire.readingDocument()) {
            @Nullable DemarshallableObject dobj = wire.getValueIn()
                    .typedMarshallable();
            assertEquals("test", dobj.name);
            assertEquals(12345, dobj.value);
        }
    }

    // Test writing and reading of a byte array with negative values to and from the Wire
    @Test
    public void testByteArrayValueWithRealBytesNegative() {
        @NotNull Wire wire = createWire();

        @NotNull final byte[] expected = {-1, -2, -3, -4, -5, -6, -7};
        wire.writeDocument(false, wir -> {
            ValueOut out = wir.writeEventName(() -> "put");
            out.swapLeaf(true);
            out.marshallable(w -> w.write(() -> "key")
                    .text("1")
                    .write(() -> "value")
                    .object(expected));
        });
        assertEquals("--- !!data\n" +
                "put: { key: \"1\", value: !byte[] !!binary //79/Pv6+Q== }\n", (Wires.fromSizePrefixedBlobs(wire.bytes())));

        wire.readDocument(null, wir -> wire.read(() -> "put")
                .marshallable(w -> w.read(() -> "key").object(Object.class, "1", Assert::assertEquals)
                        .read(() -> "value").object(byte[].class, expected, Assert::assertArrayEquals)));
    }

    // Test that ensures execution of 'testByteArrayValueWithRealBytesNegative' and then resets the wire and runs 'uint16'
    @Test
    public void two() {
        testByteArrayValueWithRealBytesNegative();
        wire.reset();
        uint16();
    }

    // Test for writing and reading byte arrays of various lengths to and from the Wire.
    @Test
    public void testByteArray() {
        // Initialize a new Wire instance.
        @NotNull Wire wire = createWire();

        // Enable padding for the Wire.
        wire.usePadding(true);

        // Write an empty byte array to the Wire.
        wire.writeDocument(false, w -> w.write("nothing").object(new byte[0]));

        // Define a byte array with one element and write it to the Wire.
        @NotNull byte[] one = {1};
        wire.writeDocument(false, w -> w.write("one").object(one));

        // Define a byte array with four elements and write it to the Wire.
        @NotNull byte[] four = {1, 2, 3, 4};
        wire.writeDocument(false, w -> w.write("four").object(four));

        // Validate the written content on the Wire.
        assertEquals("" +
                        "--- !!data\n" +
                        "nothing: !byte[] !!binary\n" +
                        "# position: 32, header: 1\n" +
                        "--- !!data\n" +
                        "one: !byte[] !!binary AQ==\n" +
                        "# position: 64, header: 2\n" +
                        "--- !!data\n" +
                        "four: !byte[] !!binary AQIDBA==\n"
                , Wires.fromSizePrefixedBlobs(wire.bytes()));

        // Read back each byte array from the Wire and verify its contents.
        wire.readDocument(null, w -> assertArrayEquals(new byte[0], (byte[]) w.read(() -> "nothing").object()));
        wire.readDocument(null, w -> assertArrayEquals(one, (byte[]) w.read(() -> "one").object()));
        wire.readDocument(null, w -> assertArrayEquals(four, (byte[]) w.read(() -> "four").object()));
    }

    // Test to ensure a map with custom marshallable keys is correctly written and read from the Wire.
    @Test
    public void testObjectKeys() {
        // Create a map with custom Marshallable objects as keys.
        @NotNull Map<MyMarshallable, String> map = new LinkedHashMap<>();
        map.put(new MyMarshallable("key1"), "value1");
        map.put(new MyMarshallable("key2"), "value2");

        // Initialize a new Wire instance.
        @NotNull Wire wire = createWire();

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
                        "}\n"
                , Wires.fromSizePrefixedBlobs(wire.bytes()));

        // Read back the map from the Wire and verify its contents.
        wire.readDocument(null, w -> {
            MyMarshallable mm = w.readEvent(MyMarshallable.class);
            assertEquals(parent.toString(), mm.toString());
            parent.equals(mm);
            assertEquals(parent, mm);
            @Nullable final Map map2 = w.getValueIn()
                    .object(Map.class);
            assertEquals(map, map2);
        });
    }

    // Test for attempting to serialize a non-serializable object (current thread).
    @Test(expected = IllegalArgumentException.class)
    public void writeUnserializable1() throws IOException {
        System.out.println(TEXT.asString(Thread.currentThread()));
    }

    // Test for attempting to serialize a non-serializable object (socket instance).
    @Test(expected = IllegalArgumentException.class)
    public void writeUnserializable2() throws IOException {
        @NotNull Socket s = new Socket();
        System.out.println(TEXT.asString(s));
    }

    // Test for attempting to serialize a non-serializable object (socket channel instance).
    @Test(expected = IllegalArgumentException.class)
    public void writeUnserializable3() throws IOException {
        SocketChannel sc = SocketChannel.open();
        System.out.println(TEXT.asString(sc));
    }

    // Test to ensure characters are correctly written to and read back from the Wire.
    @Test
    public void writeCharacter() {
        // Initialize a new Wire instance.
        @NotNull Wire wire = createWire();

        // Define a set of characters to test with.
        for (char ch : new char[]{0, '!', 'a', Character.MAX_VALUE}) {
            // Write each character to the Wire.
            wire.write().object(ch);

            // Read back each character from the Wire and verify its value.
            char ch2 = wire.read().object(char.class);
            assertEquals(ch, ch2);
        }
    }

    // Test to ensure a SortedSet is correctly written to and read from the Wire.
    @Test
    public void testSortedSet() {
        // Initialize a new Wire instance.
        @NotNull Wire wire = createWire();

        // Create a SortedSet (TreeSet) and populate it with strings.
        @NotNull SortedSet<String> set = new TreeSet<>();
        set.add("one");
        set.add("two");
        set.add("three");

        // Write the SortedSet to the Wire with the key "a".
        wire.write("a").object(set);

        // Validate the written content on the Wire.
        assertEquals("a: !!oset [\n" +
                "  one,\n" +
                "  three,\n" +
                "  two\n" +
                "]\n", wire.toString());

        // Read back the SortedSet from the Wire and validate its type and contents.
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedSet);
        assertEquals(set, o);
    }

    // Test to ensure a SortedMap is correctly written to and read from the Wire.
    @Test
    public void testSortedMap() {
        // Initialize a new Wire instance.
        @NotNull Wire wire = createWire();

        // Create a SortedMap (TreeMap) and populate it with key-value pairs.
        @NotNull SortedMap<String, Long> set = new TreeMap<>();
        set.put("one", 1L);
        set.put("two", 2L);
        set.put("three", 3L);

        // Write the SortedMap to the Wire with the key "a".
        wire.write("a").object(set);

        // Validate the written content on the Wire.
        assertEquals("a: !!omap {\n" +
                "  one: 1,\n" +
                "  three: 3,\n" +
                "  two: 2\n" +
                "}\n", wire.toString());

        // Read back the SortedMap from the Wire and validate its type and contents.
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedMap);
        assertEquals(set, o);
    }

    // Test to verify the correct deserialization of String arrays from Wire.
    @Test
    public void testStringArray() {
        // Initialize a new Wire instance and append a serialized StringArray object to it.
        @NotNull Wire wire = createWire();
        wire.bytes().append("!" + StringArray.class.getName() + " { strings: [ a, b, c ] }");

        // Deserialize the StringArray from the Wire and validate its content.
        StringArray sa = wire.getValueIn()
                .object(StringArray.class);
        assertEquals("[a, b, c]", Arrays.toString(sa.strings));

        // Repeat the test with a different serialized StringArray.
        @NotNull Wire wire2 = createWire();
        wire2.bytes().append("!" + StringArray.class.getName() + " { strings: abc }");

        // Deserialize the StringArray and validate its content.
        StringArray sa2 = wire2.getValueIn()
                .object(StringArray.class);
        assertEquals("[abc]", Arrays.toString(sa2.strings));
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
                "}\n", bw.toString());

        // Release the last acquired bytes to prevent memory leaks.
        bw.bytes.releaseLast();
    }

    /**
     * Tests the serialization and deserialization process for the DoubleWrapper class.
     * This test ensures that double values in the DoubleWrapper class are correctly serialized
     * to their engineering notation (where appropriate) and deserialized back to their original
     * values. It also tests the use of a class alias for a more concise serialization format.
     */
    @Test
    public void testDoubleEngineering() {
        // Registering an alias 'D' for the DoubleWrapper class to shorten the serialized format.
        ClassAliasPool.CLASS_ALIASES.addAlias(DoubleWrapper.class, "D");

        // Test serialization of a DoubleWrapper object with the double values in "()"
        assertEquals("!D {\n" +
                "  d: 1.0,\n" +
                "  n: -1.0\n" +
                "}\n", new DoubleWrapper(1.0).toString());
        assertEquals("!D {\n" +
                "  d: 11.0,\n" +
                "  n: -11.0\n" +
                "}\n", new DoubleWrapper(11.0).toString());
        assertEquals("!D {\n" +
                "  d: 101.0,\n" +
                "  n: -101.0\n" +
                "}\n", new DoubleWrapper(101.0).toString());
        assertEquals("!D {\n" +
                "  d: 1E3,\n" +
                "  n: -1E3\n" +
                "}\n", new DoubleWrapper(1e3)
                .toString());

        // Test deserialization: Convert the string representation back to DoubleWrapper and verify its content.
        DoubleWrapper dw = Marshallable.fromString(new DoubleWrapper(1e3).toString());
        assertEquals(1e3, dw.d, 0); // Assert the deserialized value with a delta of 0.
        assertEquals("!D {\n" +
                "  d: 10E3,\n" +
                "  n: -10E3\n" +
                "}\n", new DoubleWrapper(10e3).toString());
        DoubleWrapper dw2 = Marshallable.fromString(new DoubleWrapper(10e3).toString());
        assertEquals(10e3, dw2.d, 0);

        assertEquals("!D {\n" +
                "  d: 100E3,\n" +
                "  n: -100E3\n" +
                "}\n", new DoubleWrapper(100e3).toString());
        DoubleWrapper dw3 = Marshallable.fromString(new DoubleWrapper(100e3).toString());
        assertEquals(100e3, dw3.d, 0);

        assertEquals("!D {\n" +
                "  d: 1E6,\n" +
                "  n: -1E6\n" +
                "}\n", new DoubleWrapper(1e6).toString());
        DoubleWrapper dw4 = Marshallable.fromString(new DoubleWrapper(1e6).toString());
        assertEquals(1e6, dw4.d, 0);

        assertEquals("!D {\n" +
                "  d: 10E6,\n" +
                "  n: -10E6\n" +
                "}\n", new DoubleWrapper(10e6).toString());
        DoubleWrapper dw5 = Marshallable.fromString(new DoubleWrapper(10e6).toString());
        assertEquals(10e6, dw5.d, 0);
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
        assertEquals(expected, nl.toString());

        // Test various permutations of the NestedList's properties.
        OUTER:
        for (int i = 0; i < 64; i++) {
            Set<Integer> set = new HashSet<>();

            String cs = "!net.openhft.chronicle.wire.TextWireTest$NestedList {\n";
            int z = i;
            for (int j = 0; j < 4; j++) {
                if (!set.add(z & 3))
                    continue OUTER;
                switch (z & 3) {
                    case 0:
                        cs += "  name: name,\n";
                        break;

                    case 1:
                        cs += "  listA: [\n" +
                                "    { a: 1, b: 1.2 }\n" +
                                "  ],\n";
                        break;

                    case 2:
                        cs += "  listB: [\n" +
                                "    { a: 1, b: 1.2 },\n" +
                                "    { a: 3, b: 2.3 }\n" +
                                "  ],\n";
                        break;

                    case 3:
                        cs += "  num: 128,\n";
                        break;
                }
                z /= 4;
            }
            cs += "}\n";
            NestedList nl2 = Marshallable.fromString(cs);
            assertEquals(expected, nl2.toString());
        }
    }

    // Tests different array types using Wire that they are correctly identified and the values are correctly retrieved.
    @Test
    public void testArrayTypes() {
        // Create a Wire instance and append serialized array types and a text.
        Wire wire = createWire();
        wire.bytes().append("a: !type byte[], b: !type String[], c: hi");

        // Check that the deserialized type of "b" is String[].class.
        assertEquals(String[].class, wire.read("b").typeLiteral());

        // Check that the deserialized type of "a" is byte[].class.
        assertEquals(byte[].class, wire.read("a").typeLiteral());

        // Check that the deserialized text of "c" is "hi".
        assertEquals("hi", wire.read("c").text());
    }

    @Test
    public void testArrayTypes1() {
        // Create a Wire instance and append data to its bytes
        Wire wire = createWire();
        wire.bytes().append("a: !type [B;, b: !type String[], c: hi");

        // Verify the data types and content retrieved from the wire
        assertEquals(String[].class, wire.read("b").typeLiteral());
        assertEquals(byte[].class, wire.read("a").typeLiteral());
        assertEquals("hi", wire.read("c").text());
    }

    @Test
    public void testArrayTypes2() {
        // Iterate over a set of primitive class types
        for (Class<?> clz : new Class[]{byte.class, char.class, int.class, long.class, double.class, float.class, boolean.class}) {
            // Create a Wire instance and append data with the current class type to its bytes
            Wire wire = createWire();
            // System.out.println("Class: " + clz);
            wire.bytes().append("a: [ !type ").append(clz.getName()).append("[] ], b: !type String[], c: hi");

            // Verify the data types and content retrieved from the wire for the current class type
            assertEquals(String[].class, wire.read("b").typeLiteral());
            Collection<Class> classes = wire.read("a").typedMarshallable();
            assertArrayEquals(new Class[]{Array.newInstance(clz, 0).getClass()}, classes.toArray());
            assertEquals("hi", wire.read("c").text());
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
        assertEquals(TWTSingleton.INSTANCE, wire.read("a").object());
        assertEquals(TWTSingleton.INSTANCE, wire.read("b").object());

    }

    @Test
    public void nestedWithEnumSet() {
        // Create a Wire instance and a NestedWithEnumSet object
        Wire wire = createWire();
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
                "}\n", wire.toString());

        // Retrieve and verify the NestedWithEnumSet object from the wire
        NestedWithEnumSet a = wire.read("hello")
                .object(NestedWithEnumSet.class);
        assertEquals(n.toString(), a.toString());
        assertEquals(n, a);
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
        assertEquals(cs, o.toString());

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
                "}\n", twoLongs.toString());

        // Ensure the string representation can be correctly deserialized back to the original object
        assertEquals(twoLongs, Marshallable.fromString(twoLongs.toString()));
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
        Assert.assertEquals(d2, d, 0);
    }

    @Test
    public void testMapOfNamedKeys() {
        // Create a MapHolder and initialize its map with various implementations and contents
        MapHolder mh = new MapHolder();
        Map<RetentionPolicy, Double> map = Collections.singletonMap(RetentionPolicy.CLASS, 0.1);
        mh.map = map;
        doTestMapOfNamedKeys(mh);
        mh.map = new TreeMap<>(map);
        doTestMapOfNamedKeys(mh);
        mh.map = new HashMap<>(map);
        doTestMapOfNamedKeys(mh);
        mh.map = new LinkedHashMap<>(map);
        doTestMapOfNamedKeys(mh);
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
        assertNull(fwe.allowedFoos);
        assertEquals(OrderLevel.CHILD, fwe.orderLevel);
    }

    // Helper method to validate the map contents in a MapHolder object
    private void doTestMapOfNamedKeys(MapHolder mh) {
        assertEquals("!net.openhft.chronicle.wire.TextWireTest$MapHolder {\n" +
                        "  map: {\n" +
                        "    CLASS: 0.1\n" +
                        "  }\n" +
                        "}\n",
                TEXT.asString(mh));
    }

    @Test
    public void commaInAValue() {
        // Append a string to a new Wire instance and read the value as an object
        String text = "[1,2,3]";
        Wire wire = createWire();
        wire.bytes().append(text);
        final Object list = wire.getValueIn().object();
        assertEquals("[1,2,3]", "" + list);

        // Repeat the process with a slightly different input
        String text2 = "[ 1, 2, 3 ]";
        wire.bytes().clear().append(text2);
        final Object list2 = wire.getValueIn().object();
        assertEquals("[1, 2, 3]", "" + list2);
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
        assertEquals("[1,2,3, c]", "" + list);
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
        assertEquals(dh, dh2);
    }

    @Test
    public void readsComment() {
        StringBuilder sb = new StringBuilder();
        Wire wire = createWire();

        // Start writing to the wire
        try (DocumentContext dc = wire.writingDocument()) {
            wire.writeComment("one");
            wire.writeEventId("dto", 1);
            wire.writeComment("two");
            wire.getValueOut().object(new BinaryWireTest.DTO("text"));
            wire.writeComment("three");

            // Setup a comment listener to capture comments
            wire.commentListener(cs ->
                    sb.append(cs).append("\n"));
        }

        // Setup a method reader to capture DTOs
        final MethodReader reader = wire.methodReader((BinaryWireTest.IDTO) dto -> sb.append("dto: " + dto + "\n"));

        // Validate the read comments and DTO
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("one\n" +
                "two\n" +
                "three\n" +
                "dto: !net.openhft.chronicle.wire.BinaryWireTest$DTO {\n" +
                "  text: text\n" +
                "}\n" +
                "\n", sb.toString());
    }

    @Test
    public void readMetaData() {
        // Setup a wire with various metadata and data sections
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        wire.bytes().append("---\n" +
                "!!meta-data\n" +
                "hello-world\n" +
                "...\n" +
                "---\n" +
                "!!data\n" +
                "hello-world\n" +
                "...\n" +
                "---\n" +
                "!!meta-data\n" +
                "dto: {\n" +
                "  text: hello-world\n" +
                "}\n" +
                "...\n" +
                "---\n" +
                "!!data\n" +
                "dto: {\n" +
                "  text: hello-world\n" +
                "}\n" +
                "...\n");
        for (int i = 0; i < 4; i++) {
            try (DocumentContext dc = wire.readingDocument()) {
                // Determine if the section is metadata based on its index
                final boolean metaData = i % 2 == 0;

                // Assert if the section is identified correctly
                assertEquals("i: " + i, metaData, dc.isMetaData());
            }
        }
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
        assertArrayEquals(obj.strings, new String[] { "bar", "quux" });
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

    // Assert that the object was deserialized correctly without being affected by the comments.
        assertEquals(obj, Arrays.asList("bar", "quux"));
    }

    // Enum to demonstrate serialization of enum types
    public enum OrderLevel implements Marshallable {
        PARENT, CHILD
    }

    // Enum representing potential keys to be used in Wire data structures
    enum BWKey implements WireKey {
        field1, field2, field3
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
    public static final class FieldWithEnum extends SelfDescribingMarshallable {
        private byte[] allowedFoos;
        private final OrderLevel orderLevel = OrderLevel.PARENT;
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
        transient Map<String, Object> others = new LinkedHashMap<>();

        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            others.put(event.toString(), valueIn.object());
        }
    }

    // Class with fields of Bytes type initialized with various Byte buffers
    static class ABCD extends SelfDescribingMarshallable implements Monitorable {
        Bytes<?> A = Bytes.allocateElasticDirect();
        Bytes<?> B = Bytes.allocateDirect(64);
        Bytes<?> C = Bytes.elasticByteBuffer();
        Bytes<?> D = Bytes.allocateElasticOnHeap(1);


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
    static class ABC extends SelfDescribingMarshallable {
        StringBuilder A = new StringBuilder();
        StringBuilder B = new StringBuilder();
        StringBuilder C = new StringBuilder();
    }

    // Nested class having another nested class field and a long field
    static class NestedA extends SelfDescribingMarshallable {
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
        Bytes<?> bytes = allocateElasticDirect();

        public void bytes(@NotNull CharSequence cs) {
            bytes.clear();
            bytes.append(cs);
        }
    }

    // Class wrapping two double fields with a constructor to set them
    static class DoubleWrapper extends SelfDescribingMarshallable {
        double d;
        double n;

        public DoubleWrapper(double d) {
            this.d = d;
            this.n = -d;
        }
    }

    // Class representing a nested list structure, capable of marshallable reading.
    static class NestedList extends SelfDescribingMarshallable {
        String name;
        List<NestedItem> listA = new ArrayList<>();
        List<NestedItem> listB = new ArrayList<>();
        transient List<NestedItem> listA2 = new ArrayList<>();
        transient List<NestedItem> listB2 = new ArrayList<>();
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
    static class NestedItem extends SelfDescribingMarshallable {
        int a;
        double b;
    }

    // Class encapsulating a list of WithEnumSet instances, providing a structure for nesting.
    static class NestedWithEnumSet extends SelfDescribingMarshallable {
        List<WithEnumSet> list = new ArrayList<>();
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
        List<String> strings = new ArrayList<>();

        // Define a custom way to read objects of this type from the wire format.
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
        long hexadecimal;

        @LongConversion(HexadecimalLongConverter.class)
        long hexa2;

        // Constructor initializing both long fields.
        public TwoLongs(long hexadecimal, long hexa2) {
            this.hexadecimal = hexadecimal;
            this.hexa2 = hexa2;
        }
    }

    // Class encapsulating an integer and a Duration object, to be serialized/deserialized.
    static class DurationHolder extends SelfDescribingMarshallable {
        int foo;
        Duration duration;

        // Constructor initializing both fields.
        public DurationHolder(int foo, Duration duration) {
            this.foo = foo;
            this.duration = duration;
        }
    }

    // Basic class capable of being serialized/deserialized without field definition.
    class Circle implements Marshallable {
    }
}
