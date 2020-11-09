/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.NoBytesStore;
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.IORuntimeException;
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
import static net.openhft.chronicle.bytes.Bytes.allocateElasticDirect;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TextWireTest extends WireTestCommon {

    Bytes bytes;

    @Test
    public void testWhiteSpaceInType() {
        try {
            Object o = Marshallable.fromString("key: !" + DTO.class.getName() + " {\n" +
                    "  type:            !type               String\n" +
                    "}\n");

            assertNotNull(o);

        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testBytes() {
        @NotNull Wire wire = createWire();
        @NotNull byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;
        wire.write().bytes(NoBytesStore.NO_BYTES)
                .write().bytes(Bytes.wrapForRead("Hello".getBytes(ISO_8859_1)))
                .write().bytes(Bytes.wrapForRead("quotable, text".getBytes(ISO_8859_1)))
                .write().bytes(allBytes);
        System.out.println(bytes.toString());
        @NotNull Bytes allBytes2 = allocateElasticOnHeap();
        wire.read().bytes(b -> assertEquals(0, b.readRemaining()))
                .read().bytes(b -> assertEquals("Hello", b.toString()))
                .read().bytes(b -> assertEquals("quotable, text", b.toString()))
                .read().bytes(allBytes2);
        assertEquals(Bytes.wrapForRead(allBytes), allBytes2);
    }

    @Test
    public void comment() {
        @NotNull Wire wire = createWire();
        wire.writeComment("\thi: omg");
        wire.write("hi").text("there");
        assertEquals("there", wire.read("hi").text());
    }

    @Test
    public void testTypeInsteadOfField() {
        Wire wire = TextWire.from("!!null \"\"");
        StringBuilder sb = new StringBuilder();
        wire.read(sb).object(Object.class);
        assertEquals(0, sb.length());
    }

    @Test
    public void testFieldWithComment() {
        FieldWithComment f = new FieldWithComment();
        f.field = "hello world";
        Assert.assertEquals("!net.openhft.chronicle.wire.TextWireTest$FieldWithComment {\n" +
                "  field: hello world, \t\t# a comment where the value=hello world\n" +
                "\n" +
                "}\n", Marshallable.$toString(f));
    }

    @Test
    public void testFieldWithComment2() {
        FieldWithComment2 f = new FieldWithComment2();
        f.field = "hello world";
        Assert.assertEquals("!net.openhft.chronicle.wire.TextWireTest$FieldWithComment2 {\n" +
                "  field: hello world, \t\t# a comment where the value=hello world\n" +
                "  field2: !!null \"\"\n" +
                "}\n", Marshallable.$toString(f));
    }

    @Test
    public void handleUnexpectedFields() {
        TwoFields tf = Marshallable.fromString("!" + TwoFields.class.getName() + " {" +
                "a: 1,\n" +
                "b: two,\n" +
                "c: three,\n" +
                "d: 44,\n" +
                "e: also,\n" +
                "f: at the end\n" +
                "}");
        assertEquals("a=1\n" +
                        "c=three\n" +
                        "e=also\n" +
                        "f=at the end",
                asProperties(tf.others));

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

    public String asProperties(Map<String, Object> map) {
        return map.entrySet().stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    @Test
    public void licenseCheck() {
        WireType.TEXT.licenceCheck();
        assertTrue(WireType.TEXT.isAvailable());

        try {
            WireType.DELTA_BINARY.licenceCheck();
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
        assertFalse(WireType.DELTA_BINARY.isAvailable());
    }

    @Test
    public void writeObjectWithTreeMap() {
        @NotNull Wire wire = createWire();
        ObjectWithTreeMap value = new ObjectWithTreeMap();
        value.map.put("hello", "world");
        wire.write().object(value);

        System.out.println(wire);

        ObjectWithTreeMap value2 = new ObjectWithTreeMap();
        wire.read().object(value2, ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value2.map.toString());

        wire.bytes().readPosition(0);
        ObjectWithTreeMap value3 = new ObjectWithTreeMap();
        wire.read().object(value3, Object.class);
        assertEquals("{hello=world}", value3.map.toString());

        wire.bytes().readPosition(0);
        ObjectWithTreeMap value4 = wire.read().object(ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value4.map.toString());
    }

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
        Assert.assertTrue(w instanceof Map);
    }

    @Test
    public void testFromString2() {
        for (int i = 0; i <= 256; i++) {
            Wire w = TextWire.from(
                    "data: 0x" + Integer.toHexString(i).toUpperCase() + ",\n" +
                            "data2: 0x" + Integer.toHexString(i).toLowerCase());
            assertEquals(i, w.read("data").int64());
            assertEquals(i, w.read("data2").int64());
        }
    }

    @Test
    public void testWriteToBinaryAndTriesToConvertToText() {

        Bytes b = Bytes.elasticByteBuffer();
        Wire wire = WireType.BINARY.apply(b);
        @NotNull Map<String, String> data = Collections.singletonMap("key", "value");

        @NotNull HashMap map = new HashMap();
        map.put("some", data);
        map.put("some-other", data);

        try (DocumentContext dc = wire.writingDocument()) {
            wire.write("map").object(map);
        }

        final String textYaml = Wires.fromSizePrefixedBlobs(b);
        System.out.println(textYaml);
        @Nullable Object o = WireType.TEXT.fromString(textYaml);
        Assert.assertEquals("{map={some={key=value}, some-other={key=value}}}", o.toString());

        b.releaseLast();
    }

    @Test
    public void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        assertEquals("\"\": \"\": \"\": ", wire.toString());

        wire.bytes().releaseLast();
    }

    @NotNull
    private TextWire createWire() {
        bytes = allocateElasticOnHeap();
        return new TextWire(bytes);
    }

    @Test
    public void testSimpleBool() {
        @NotNull Wire wire = createWire();

        wire.write(() -> "F").bool(false);
        wire.write(() -> "T").bool(true);
        assertEquals("F: false\n" +
                "T: true\n", wire.toString());
        @NotNull String expected = "{F=false, T=true}";
        expectWithSnakeYaml(expected, wire);

        assertFalse(wire.read(() -> "F").bool());
        assertTrue(wire.read(() -> "T").bool());
    }

    @Test
    public void testFailingBool() {
        @NotNull Wire wire = createWire();

        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");
        assertEquals("A: \"\"\n" +
                "B: other\n", wire.toString());
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        assertFalse(wire.read(() -> "A").bool());
        assertFalse(wire.read(() -> "B").bool());
    }

    @Test
    public void testFailingBoolean() {
        @NotNull Wire wire = createWire();

        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");
        assertEquals("A: \"\"\n" +
                "B: other\n", wire.toString());
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        // TODO fix.
//        assertEquals(null, wire.read(() -> "A").object(Boolean.class));
        assertEquals(false, wire.read(() -> "B").object(Boolean.class));
    }

    @Test
    public void testLeadingSpace() {
        @NotNull Wire wire = createWire();
        wire.write().text(" leadingspace");
        assertEquals(" leadingspace", wire.read().text());
    }

    private void expectWithSnakeYaml(String expected, @NotNull Wire wire) {
        String s = wire.toString();
        @Nullable Object load = null;
        try {
            @NotNull Yaml yaml = new Yaml();
            load = yaml.load(new StringReader(s));
        } catch (Exception e) {
            System.out.println(s);
            throw e;
        }
        assertEquals(expected, load.toString());
    }

    @Test
    public void testInt64() {
        @NotNull Wire wire = createWire();
        long expected = 1234567890123456789L;
        wire.write(() -> "VALUE").int64(expected);
        expectWithSnakeYaml("{VALUE=1234567890123456789}", wire);
        assertEquals(expected, wire.read(() -> "VALUE").int64());
    }

    @Test
    public void testInt16() {
        @NotNull Wire wire = createWire();
        short expected = 12345;
        wire.write(() -> "VALUE").int64(expected);
        expectWithSnakeYaml("{VALUE=12345}", wire);
        assertEquals(expected, wire.read(() -> "VALUE").int16());
    }

    @Test(expected = IllegalStateException.class)
    public void testInt16TooLarge() {
        @NotNull Wire wire = createWire();
        wire.write(() -> "VALUE").int64(Long.MAX_VALUE);
        wire.read(() -> "VALUE").int16();
    }

    @Test
    public void testInt32() {
        @NotNull Wire wire = createWire();
        int expected = 1;
        wire.write(() -> "VALUE").int64(expected);
        wire.write(() -> "VALUE2").int64(expected);
        expectWithSnakeYaml("{VALUE=1, VALUE2=1}", wire);
//        System.out.println("out" + Bytes.toHexString(wire.bytes()));
        assertEquals(expected, wire.read(() -> "VALUE").int16());
        assertEquals(expected, wire.read(() -> "VALUE2").int16());
    }

    @Test(expected = IllegalStateException.class)
    public void testInt32TooLarge() {
        @NotNull Wire wire = createWire();
        wire.write(() -> "VALUE").int64(Integer.MAX_VALUE);
        wire.read(() -> "VALUE").int16();
    }

    @Test
    public void testWrite1() {
        @NotNull Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        assertEquals("field1: field2: field3: ", wire.toString());
    }

    @Test
    public void testWrite2() {
        @NotNull Wire wire = createWire();
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");
        assertEquals("Hello: World: \"Long field name which is more than 32 characters, Bye\": ", wire.toString());
    }

    @Test
    public void testRead() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");
        wire.read();
        wire.read();
        wire.read();
        assertEquals(1, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead1() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        wire.read(BWKey.field1);
        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead2() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        @NotNull String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name1);

        // ok as blank matches anything
        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name);
        assertEquals(0, name.length());

        wire.read(name);
        assertEquals(BWKey.field1.name(), name.toString());

        wire.read(name);
        assertEquals(name1, name.toString());

        assertEquals(1, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int8() {
        @NotNull Wire wire = createWire();
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);
        wire.write(() -> "Test").int8(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int8(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int16() {
        @NotNull Wire wire = createWire();
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);
        wire.write(() -> "Test").int16(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint8() {
        @NotNull Wire wire = createWire();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write(() -> "Test").uint8(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint8(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint16() {
        @NotNull Wire wire = createWire();
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write(() -> "Test").uint16(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint32() {
        @NotNull Wire wire = createWire();
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write(() -> "Test").uint32(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        @NotNull AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int32() {
        @NotNull Wire wire = createWire();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write(() -> "Test").int32(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int64() {
        @NotNull Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        @NotNull AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void float64() {
        @NotNull Wire wire = createWire();
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write(() -> "Test").float64(3);
        assertEquals("\"\": 1.0\n" +
                "field1: 2.0\n" +
                "Test: 3.0\n", wire.toString());
        expectWithSnakeYaml("{=1.0, field1=2.0, Test=3.0}", wire);

        // ok as blank matches anything
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
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void text() {
        @NotNull Wire wire = createWire();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        @NotNull String name = "Long field name which is more than 32 characters, \\ \nBye";

        wire.write(() -> "Test")
                .text(name);
        expectWithSnakeYaml("{=Hello, field1=world, Test=Long field name which is more than 32 characters, \\ \n" +
                "Bye}", wire);
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, \\\\ \\nBye\"\n", wire.toString());

        // ok as blank matches anything
        @NotNull StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name).forEach(e -> {
            assertNotNull(wire.read().textTo(sb));
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void type() {
        @NotNull Wire wire = createWire();
        wire.write().typePrefix("MyType");
        wire.write(BWKey.field1).typePrefix("AlsoMyType");
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write(() -> "Test").typePrefix(name1);
        wire.writeComment("");
        // TODO fix how types are serialized.
//        expectWithSnakeYaml(wire, "{=1, field1=2, Test=3}");
        assertEquals("\"\": !MyType " +
                "field1: !AlsoMyType " +
                "Test: !" + name1 + " # \n", wire.toString());

        // ok as blank matches anything
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().typePrefix(e, Assert::assertEquals);
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testTypeWithEmpty() {
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedA.class, NestedB.class);
        NestedA a = Marshallable.fromString("!NestedA {\n" +
                "  b: !NestedB,\n" +
                "  value: 12345\n" +
                "}");
        assertEquals("!NestedA {\n" +
                "  b: {\n" +
                "    field1: 0.0\n" +
                "  },\n" +
                "  value: 12345\n" +
                "}\n", a.toString());
    }

    @Test
    public void testSingleQuote() {
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedA.class);
        NestedA a = Marshallable.fromString("!NestedA {\n" +
                "  b: !NestedB,\n" +
                "  value: 12345\n" +
                "}");
        assertNotNull(a);
    }

    @Test
    public void testBool() {
        @NotNull Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);
        wire.read().bool(false, Assert::assertEquals)
                .read().bool(true, Assert::assertEquals)
                .read().bool(null, Assert::assertEquals);
    }

    @Test
    public void testFloat32() {
        @NotNull Wire wire = createWire();
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY)
                .write().float32(Float.NEGATIVE_INFINITY)
                .write().float32(123456.0f);
        wire.read().float32(this, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(this, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(this, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(Float.NEGATIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(123456.0f, t, 0.0F));
    }

    @Test
    public void testTime() {
        @NotNull Wire wire = createWire();
        LocalTime now = LocalTime.now();
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);
        assertEquals("\"\": " + now + "\n" +
                        "\"\": 23:59:59.999999999\n" +
                        "\"\": 00:00\n",
                bytes.toString());
        wire.read().time(now, Assert::assertEquals)
                .read().time(LocalTime.MAX, Assert::assertEquals)
                .read().time(LocalTime.MIN, Assert::assertEquals);
    }

    @Test
    public void testZonedDateTime() {
        @NotNull Wire wire = createWire();
        ZonedDateTime now = ZonedDateTime.now();
        ZoneId zone = ZoneId.of("Europe/London");
        final ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, zone);
        final ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, zone);
        wire.write()
                .zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);
        assertEquals("\"\": \"" + now + "\"\n" +
                "\"\": \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                "\"\": \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString());
        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(max, Assert::assertEquals)
                .read().zonedDateTime(min, Assert::assertEquals);

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

    @Test
    public void testDate() {
        @NotNull Wire wire = createWire();
        LocalDate now = LocalDate.now();
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);
        wire.read().date(now, Assert::assertEquals)
                .read().date(LocalDate.MAX, Assert::assertEquals)
                .read().date(LocalDate.MIN, Assert::assertEquals);
    }

    @Test
    public void testUuid() {
        @NotNull Wire wire = createWire();
        UUID uuid = UUID.randomUUID();
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));
        wire.read().uuid(uuid, Assert::assertEquals)
                .read().uuid(new UUID(0, 0), Assert::assertEquals)
                .read().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), Assert::assertEquals);
    }

    @Test
    public void testTypeWithoutSpace() {
        @NotNull Wire wire = createWire();
        wire.bytes().append("A: !").append(MyTypes.class.getName()).append("{}");

        @NotNull MyTypes mt = (MyTypes) wire.read(() -> "A").object();
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
        @NotNull Wire wire = createWire();
        wire.bytes().append(
                "A: NaN,\n" +
                        "A2: NaN ,\n" +
                        "A3: Infinity,\n" +
                        "A4: -Infinity,\n" +
                        "A5: NaN\n" +
                        "B: 1.23\n");
        assertEquals(Double.NaN, wire.read("A").float64(), 0);
        assertEquals(Double.NaN, wire.read("A2").float64(), 0);
        assertEquals(Double.POSITIVE_INFINITY, wire.read("A3").float64(), 0);
        assertEquals(Double.NEGATIVE_INFINITY, wire.read("A4").float64(), 0);
        assertEquals(Double.NaN, wire.read("A5").float64(), 0);
        assertEquals(1.23, wire.read("B").float64(), 0);
    }

    @Test
    public void testABCDBytes() {
        @NotNull Wire wire = createWire();
        wire.bytes().append(
                "A: \"hi\",\n" +
                        "B: 'hi',\n" +
                        "C: hi,\n" +
                        "D: bye,\n");
        ABCD abcd = new ABCD();

        try {
            for (int i = 0; i < 5; i++) {
                wire.bytes().readPosition(0);
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
            abcd.releaseAll();

            WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(ABCD.class);
            ABCD abcd0 = (ABCD) wm.defaultValue();
            abcd0.releaseAll();
        }
    }

    @Test
    public void testABCStringBuilder() {

        @NotNull Wire wire = createWire();
        wire.bytes().append(
                "A: \"hi\",\n" +
                        "B: 'hi',\n" +
                        "C: hi,\n");
        ABC abc = new ABC();

        for (int i = 0; i < 5; i++) {
            wire.bytes().readPosition(0);
            assertEquals("!net.openhft.chronicle.wire.TextWireTest$ABC {\n" +
                    "  A: hi,\n" +
                    "  B: hi,\n" +
                    "  C: hi\n" +
                    "}\n", wire.getValueIn()
                    .object(abc, ABC.class)
                    .toString());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testMapReadAndWriteStrings() {
        @NotNull final Bytes bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = new TextWire(bytes);

        @NotNull final Map<String, String> expected = new LinkedHashMap<>();

        expected.put("hello", "world");
        expected.put("hello1", "world1");
        expected.put("hello2", "world2");

        wire.writeDocument(false, o -> {
            o.writeEventName(() -> "example")
                    .map(expected);
        });
//        bytes.readPosition(4);
//        expectWithSnakeYaml("{example={hello=world, hello1=world1, hello2=world2}}", wire);
//        bytes.readPosition(0);
        assertEquals("--- !!data\n" +
                        "example: {\n" +
                        "  hello: world,\n" +
                        "  hello1: world1,\n" +
                        "  hello2: world2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes));
        @NotNull final Map<String, String> actual = new LinkedHashMap<>();
        wire.readDocument(null, c -> c.read(() -> "example").map(actual));
        assertEquals(expected, actual);
    }

    @Test
    @Ignore("unreleased bytes")
    public void testBytesField() {
        DtoWithBytesField dto = new DtoWithBytesField(), dto2 = null;
        byte[] binaryData = new byte[]{1, 2, 3, 4};
        dto.bytes = Bytes.wrapForRead(binaryData);
        dto.another = 123L;

        try {
            String cs = dto.toString();
            System.out.println(cs);
            dto2 = Marshallable.fromString(cs);
            assertEquals(cs, dto2.toString());
        } finally {
            dto.bytes.releaseLast();
            dto2.bytes.releaseLast();
        }
    }

    @Test
    public void testWriteMarshallable() {
        @NotNull Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;
        mtA.text.append("Hello World");

        wire.write(() -> "A").marshallable(mtA);

        @NotNull MyTypesCustom mtB = new MyTypesCustom();
        mtB.flag = false;
        mtB.d = 123.4567;
        mtB.i = -123457890;
        mtB.s = (short) 1234;
        mtB.text.append("Bye now");
        wire.write(() -> "B").marshallable(mtB);

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
        wire.read(() -> "A").marshallable(mt2);
        assertEquals(mt2, mtA);

        wire.read(() -> "B").marshallable(mt2);
        assertEquals(mt2, mtB);
    }

    @Test
    public void testWriteMarshallableAndFieldLength() {
        @NotNull Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;

        @NotNull ValueOut write = wire.write(() -> "A");

        long start = wire.bytes().writePosition() + 1; // including one space for "sep".
        write.marshallable(mtA);
        long fieldLen = wire.bytes().writePosition() - start;

        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=}}", wire);

        @NotNull ValueIn read = wire.read(() -> "A");

        long len = read.readLength();

        assertEquals(fieldLen, len, 1);
    }

    @SuppressWarnings("deprecation")
    @Test
    @Ignore
    public void testMapReadAndWriteIntegers() {
        @NotNull final Bytes bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = new TextWire(bytes);

        @NotNull final Map<Integer, Integer> expected = new HashMap<>();

        expected.put(1, 11);
        expected.put(2, 2);
        expected.put(3, 3);

        wire.writeDocument(false, o -> {
            o.write(() -> "example").map(expected);
        });

        assertEquals("--- !!data\n" +
                "example: {\n" +
                "  ? !int 1: !int 11,\n" +
                "  ? !int 2: !int 2,\n" +
                "  ? !int 3: !int 3\n" +
                "}\n", Wires.fromSizePrefixedBlobs(bytes));
        @NotNull final Map<Integer, Integer> actual = new HashMap<>();
        wire.readDocument(null, c -> {
            @Nullable Map m = c.read(() -> "example").map(Integer.class, Integer.class, actual);
            assertEquals(m, expected);
        });

        wire.bytes().readPosition(0);
        // skip the length
        wire.bytes().readSkip(4);
        // TODO: snakeyaml doesn't like !int
        // Can't construct a java object for !int; exception=Invalid tag: !int
        //   in 'reader', line 2, column 5:
        //     ? !int 1: !int 11,
        //       ^
        expectWithSnakeYaml("{1=11, 2=2, 3=3}", wire);
    }

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

    @SuppressWarnings("deprecation")
    @Test
    public void testMapReadAndWriteMarshable() {
        @NotNull final Bytes bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = new TextWire(bytes);

        @NotNull final Map<MyMarshallable, MyMarshallable> expected = new LinkedHashMap<>();

        expected.put(new MyMarshallable("aKey"), new MyMarshallable("aValue"));
        expected.put(new MyMarshallable("aKey2"), new MyMarshallable("aValue2"));

        wire.writeDocument(false, o -> o.write(() -> "example").marshallable(expected, MyMarshallable.class, MyMarshallable.class, true));

        assertEquals("--- !!data\n" +
                        "example: {\n" +
                        "  ? { MyField: aKey }: { MyField: aValue },\n" +
                        "  ? { MyField: aKey2 }: { MyField: aValue2 }\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(bytes));
        @NotNull final Map<MyMarshallable, MyMarshallable> actual = new LinkedHashMap<>();

        wire.readDocument(null, c -> c.read(() -> "example")
                .map(
                        MyMarshallable.class,
                        MyMarshallable.class,
                        actual));

        assertEquals(expected, actual);

        wire.bytes().releaseLast();
    }

    static class DTO extends SelfDescribingMarshallable {
        Class type;
    }

    @Test
    public void testException() {
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
        @NotNull final Bytes bytes = allocateElasticOnHeap();
        @NotNull final Wire wire = new TextWire(bytes);
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

        wire.readDocument(null, r -> {
            Throwable t = r.read(() -> "exception").throwable(true);
            assertTrue(t instanceof InvalidAlgorithmParameterException);
        });
    }

    @Test
    public void testEnum() {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class, "WireType");

        @NotNull Wire wire = createWire();
        wire.write().object(WireType.BINARY)
                .write().object(TEXT)
                .write().object(WireType.RAW);

        assertEquals("\"\": !WireType BINARY\n" +
                "\"\": !WireType TEXT\n" +
                "\"\": !WireType RAW\n", bytes.toString());

        assertEquals(WireType.BINARY, wire.read().object(Object.class));
        assertEquals(TEXT, wire.read().object(Object.class));
        assertEquals(WireType.RAW, wire.read().object(Object.class));
    }

    @Test
    public void testArrays() {
        @NotNull Wire wire = createWire();

        @NotNull Object[] noObjects = {};
        wire.write("a").object(noObjects);

        assertEquals("a: []", wire.toString());

        @Nullable Object[] object = wire.read().object(Object[].class);
        assertEquals(0, object.length);

        wire.clear();

        @NotNull Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write("b").object(threeObjects);

        assertEquals("b: [\n" +
                "  abc,\n" +
                "  def,\n" +
                "  ghi\n" +
                "]", wire.toString());

        @Nullable Object[] object2 = wire.read()
                .object(Object[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    @Test
    public void testArrays2() {
        @NotNull Wire wire = createWire();
        @NotNull Object[] a1 = new Object[0];
        wire.write("empty").object(a1);
        @NotNull Object[] a2 = {1L};
        wire.write("one").object(a2);
        @NotNull Object[] a3 = {"Hello", 123, 10.1};
        wire.write("three").object(Object[].class, a3);

        System.out.println(wire);
        @Nullable Object o1 = wire.read()
                .object(Object[].class);
        assertArrayEquals(a1, (Object[]) o1);
        @Nullable Object o2 = wire.read().object(Object[].class);
        assertArrayEquals(a2, (Object[]) o2);
        @Nullable Object o3 = wire.read().object(Object[].class);
        assertArrayEquals(a3, (Object[]) o3);
    }

    @Test
    public void testGZIPCompressionAsText() {
        @NotNull Wire wire = createWire();
        @NotNull final String s = "xxxxxxxxxxx1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        @NotNull String str = s + s + s + s;

        @NotNull byte[] compressedBytes = str.getBytes(ISO_8859_1);
        wire.write().compress("gzip", Bytes.wrapForRead(compressedBytes));

        @NotNull Bytes bytes = allocateElasticOnHeap();
        wire.read().bytes(bytes);
        assertEquals(str, bytes.toString());
    }

    @Test
    public void testLZWCompressionAsText() {
        @NotNull Wire wire = createWire();
        @NotNull final String s = "xxxxxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxx";
        @NotNull String str = s + s + s + s;

        @NotNull byte[] compressedBytes = str.getBytes(ISO_8859_1);
        wire.write().compress("lzw", Bytes.wrapForRead(compressedBytes));

        @NotNull Bytes bytes = allocateElasticOnHeap();
        wire.read().bytes(bytes);
        assertEquals(str, bytes.toString());
    }

    @Test
    public void testStringArrays() {
        @NotNull Wire wire = createWire();

        @NotNull String[] noObjects = {};
        wire.write().object(noObjects);

        @Nullable String[] object = wire.read().object(String[].class);
        assertEquals(0, object.length);

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        @NotNull String[] threeObjects = {"abc", "def", "ghi"};
        wire.write().object(threeObjects);

        @Nullable String[] object2 = wire.read()
                .object(String[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    @Test
    public void testStringList() {
        @NotNull Wire wire = createWire();

        @NotNull List<String> noObjects = new ArrayList();
        wire.write().object(noObjects);

        @Nullable List<String> list = wire.read().object(List.class);
        assertEquals(0, list.size());

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        @NotNull List<String> threeObjects = Arrays.asList("abc", "def", "ghi");
        wire.write().object(threeObjects);

        @Nullable List<String> list2 = wire.read()
                .object(List.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }

    @Test
    public void testStringSet() {
        @NotNull Wire wire = createWire();

        @NotNull Set<String> noObjects = new HashSet();
        wire.write().object(noObjects);

        @Nullable Set<String> list = wire.read().object(Set.class);
        assertEquals(0, list.size());

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        @NotNull Set<String> threeObjects = new HashSet(Arrays.asList("abc", "def", "ghi"));
        wire.write().object(threeObjects);

        @Nullable Set<String> list2 = wire.read()
                .object(Set.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }

    @Test
    @Ignore
    public void testStringMap() {
        @NotNull Wire wire = createWire();

        @NotNull Map<String, String> noObjects = new HashMap();
        wire.write().object(noObjects);

        @Nullable Map<String, String> map = wire.read().object(Map.class);
        assertEquals(0, map.size());

//        // TODO we shouldn't need to create a new wire.
//        wire = createWire();
//
//        Set<String> threeObjects = new HashSet(Arrays.asList(new String[]{"abc", "def", "ghi"}));
//        wire.write().object(threeObjects);
//
//        Set<String> list2 = wire.read()
//                .object(Set.class);
//        assertEquals(3, list2.size());
//        assertEquals("[abc, def, ghi]", list2.toString());
    }

    @Test
    public void testNestedDecode() {
        @NotNull String s = "cluster: {\n" +
                "  host1: {\n" +
                "     hostId: 1,\n" +
//                "     name: one,\n" +
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
        ObjIntConsumer<String> results = EasyMock.createMock(ObjIntConsumer.class);
        results.accept("host1", 1);
        results.accept("host2", 2);
        results.accept("host4", 4);
        replay(results);
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
        verify(results);
    }

    @Test
    public void writeNull() {
        @NotNull Wire wire = createWire();
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        @Nullable Object o = wire.read().object(Object.class);
        assertNull(o);
        @Nullable String s = wire.read().object(String.class);
        assertNull(s);
        @Nullable RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        assertNull(rp);
        @Nullable Circle c = wire.read().object(Circle.class);
        assertNull(c);
    }

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

    @Test
    public void readDemarshallable() {
        @NotNull Wire wire = createWire().useBinaryDocuments();
        try (DocumentContext $ = wire.writingDocument(true)) {
            wire.getValueOut().typedMarshallable(new DemarshallableObject("test", 12345));
        }

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

    @Test
    public void testByteArray() {
        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("nothing").object(new byte[0]));
        @NotNull byte[] one = {1};
        wire.writeDocument(false, w -> w.write("one").object(one));
        @NotNull byte[] four = {1, 2, 3, 4};
        wire.writeDocument(false, w -> w.write("four").object(four));

        assertEquals("--- !!data\n" +
                        "nothing: !byte[] !!binary \n" +
                        "# position: 31, header: 1\n" +
                        "--- !!data\n" +
                        "one: !byte[] !!binary AQ==\n" +
                        "# position: 62, header: 2\n" +
                        "--- !!data\n" +
                        "four: !byte[] !!binary AQIDBA==\n"
                , Wires.fromSizePrefixedBlobs(wire.bytes()));
        wire.readDocument(null, w -> assertArrayEquals(new byte[0], (byte[]) w.read(() -> "nothing").object()));
        wire.readDocument(null, w -> assertArrayEquals(one, (byte[]) w.read(() -> "one").object()));
        wire.readDocument(null, w -> assertArrayEquals(four, (byte[]) w.read(() -> "four").object()));
    }

    @Test
    public void testObjectKeys() {
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
                        "}\n"
                , Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, w -> {
            MyMarshallable mm = w.readEvent(MyMarshallable.class);
            assertEquals(parent.toString(), mm.toString());
            parent.equals(mm);
            assertEquals(parent, mm);
            @Nullable final Map map2 = w.getValueIn()
                    .object(Map.class);
            assertEquals(map, map2);
        });

        wire.bytes().releaseLast();
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeUnserializable() throws IOException {
        System.out.println(TEXT.asString(Thread.currentThread()));
        @NotNull Socket s = new Socket();
        System.out.println(TEXT.asString(s));
        SocketChannel sc = SocketChannel.open();
        System.out.println(TEXT.asString(sc));
    }

    @Test
    public void writeCharacter() {
        @NotNull Wire wire = createWire();
        for (char ch : new char[]{0, '!', 'a', Character.MAX_VALUE}) {
            wire.write().object(ch);
            char ch2 = wire.read().object(char.class);
            assertEquals(ch, ch2);
        }
    }

    @Test
    public void testSortedSet() {
        @NotNull Wire wire = createWire();
        @NotNull SortedSet<String> set = new TreeSet<>();
        set.add("one");
        set.add("two");
        set.add("three");
        wire.write("a").object(set);
        assertEquals("a: !!oset [\n" +
                "  one,\n" +
                "  three,\n" +
                "  two\n" +
                "]", wire.toString());
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedSet);
        assertEquals(set, o);
    }

    @Test
    public void testSortedMap() {
        @NotNull Wire wire = createWire();
        @NotNull SortedMap<String, Long> set = new TreeMap<>();
        set.put("one", 1L);
        set.put("two", 2L);
        set.put("three", 3L);
        wire.write("a").object(set);
        assertEquals("a: !!omap {\n" +
                "  one: 1,\n" +
                "  three: 3,\n" +
                "  two: 2\n" +
                "}\n", wire.toString());
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedMap);
        assertEquals(set, o);
    }

    @Test
    public void testStringArray() {
        @NotNull Wire wire = createWire();
        wire.bytes().append("!" + StringArray.class.getName() + " { strings: [ a, b, c ] }");
        StringArray sa = wire.getValueIn()
                .object(StringArray.class);
        assertEquals("[a, b, c]", Arrays.toString(sa.strings));

        @NotNull Wire wire2 = createWire();
        wire2.bytes().append("!" + StringArray.class.getName() + " { strings: abc }");
        StringArray sa2 = wire2.getValueIn()
                .object(StringArray.class);
        assertEquals("[abc]", Arrays.toString(sa2.strings));
    }

    @Test
    public void testSetBytesAfterDeserialization() {
        BytesWrapper bw = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireTest$BytesWrapper {\n" +
                "  bytes: \"\"\n" +
                "}\n");
        bw.bytes("");
        bw.bytes("hi");
        bw.bytes("hello");
        assertEquals("!net.openhft.chronicle.wire.TextWireTest$BytesWrapper {\n" +
                "  bytes: hello\n" +
                "}\n", bw.toString());
        bw.bytes.releaseLast();
    }

    @Test
    public void testDoubleEngineering() {
        ClassAliasPool.CLASS_ALIASES.addAlias(DoubleWrapper.class, "D");
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
        DoubleWrapper dw = Marshallable.fromString(new DoubleWrapper(1e3).toString());
        assertEquals(1e3, dw.d, 0);
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

    @Test
    public void testNestedList() {
        NestedList nl = Marshallable.fromString("!" + NestedList.class.getName() + " {\n" +
                "  name: name,\n" +
                "  listA: [ { a: 1, b: 1.2 } ],\n" +
                "  listB: [ { a: 1, b: 1.2 }, { a: 3, b: 2.3 } ]," +
                "  num: 128\n" +
                "}\n");
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
        assertEquals(expected, nl.toString());

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

    @Test
    public void testArrayTypes() {
        Wire wire = createWire();
        wire.bytes().append("a: !type byte[], b: !type String[], c: hi");

        assertEquals(String[].class, wire.read("b").typeLiteral());
        assertEquals(byte[].class, wire.read("a").typeLiteral());
        assertEquals("hi", wire.read("c").text());
    }

    @Test
    public void testArrayTypes1() {
        Wire wire = createWire();
        wire.bytes().append("a: !type [B;, b: !type String[], c: hi");

        assertEquals(String[].class, wire.read("b").typeLiteral());
        assertEquals(byte[].class, wire.read("a").typeLiteral());
        assertEquals("hi", wire.read("c").text());
    }

    @Test
    public void testArrayTypes2() {
        for (Class<?> clz : new Class[]{byte.class, char.class, int.class, long.class, double.class, float.class, boolean.class}) {
            Wire wire = createWire();
            System.out.println("Class: " + clz);
            wire.bytes().append("a: [ !type ").append(clz.getName()).append("[] ], b: !type String[], c: hi");

            assertEquals(String[].class, wire.read("b").typeLiteral());
            Collection<Class> classes = wire.read("a").typedMarshallable();
            assertArrayEquals(new Class[]{Array.newInstance(clz, 0).getClass()}, classes.toArray());
            assertEquals("hi", wire.read("c").text());
        }
    }

    @Test
    public void readMarshallableAsEnum() {
        Wire wire = createWire();
        ClassAliasPool.CLASS_ALIASES.addAlias(TWTSingleton.class);
        wire.bytes().append("a: !TWTSingleton { },\n" +
                "b: !TWTSingleton {\n" +
                "}\n");
        assertEquals(TWTSingleton.INSTANCE, wire.read("a").object());
        assertEquals(TWTSingleton.INSTANCE, wire.read("b").object());

    }

    @Test
    public void nestedWithEnumSet() {
        Wire wire = createWire();
        NestedWithEnumSet n = new NestedWithEnumSet();
        n.list.add(new WithEnumSet("none"));
        n.list.add(new WithEnumSet("one", EnumSet.of(TimeUnit.DAYS)));
        n.list.add(new WithEnumSet("two", EnumSet.of(TimeUnit.DAYS, TimeUnit.HOURS)));
        wire.write("hello")
                .object(NestedWithEnumSet.class, n);
        assertEquals("hello: {\n" +
                "  list: [\n" +
                "    { name: none },\n" +
                "    { name: one, timeUnits: [ DAYS ] },\n" +
                "    { name: two, timeUnits: [ HOURS, DAYS ] }\n" +
                "  ]\n" +
                "}\n", wire.toString());

        NestedWithEnumSet a = wire.read("hello")
                .object(NestedWithEnumSet.class);
        assertEquals(n.toString(), a.toString());
        assertEquals(n, a);
    }

    @Test
    public void testParse2() {

        MyDto myDto1 = new MyDto();

        myDto1.strings.add("hello");
        myDto1.strings.add("world");

        String cs = myDto1.toString();
        System.out.println(cs);
        MyDto o = Marshallable.fromString(cs);
        assertEquals(cs, o.toString());

        assert o.strings.size() == 2;
    }

    @Test
    public void longConverter() {
        TwoLongs twoLongs = new TwoLongs(0x1234567890abcdefL, -1);
        assertEquals("!net.openhft.chronicle.wire.TextWireTest$TwoLongs {\n" +
                "  hexadecimal: 1234567890abcdef,\n" +
                "  hexa2: ffffffffffffffff\n" +
                "}\n", twoLongs.toString());
        assertEquals(twoLongs, Marshallable.fromString(twoLongs.toString()));
    }

    @Test
    public void testDoublePrecisionOverTextWire() {
        final Bytes<?> bytes = Wires.acquireBytes();

        final Wire wire = WireType.TEXT.apply(bytes);
        final double d = 0.000212345678901;
        wire.getValueOut().float64(d);

        final TextWire textWire = TextWire.from(bytes.toString());
        final double d2 = textWire.getValueIn().float64();

        Assert.assertEquals(d2, d, 0);
    }

    enum BWKey implements WireKey {
        field1, field2, field3
    }

    static class FieldWithComment extends SelfDescribingMarshallable {
        @Comment("a comment where the value=%s")
        String field;
        //    String field2;
    }

    static class FieldWithComment2 extends SelfDescribingMarshallable {
        @Comment("a comment where the value=%s")
        String field;
        String field2;
    }

    static class TwoFields extends AbstractMarshallableCfg {
        String b;
        int d;
        int notThere;

        transient Map<String, Object> others = new LinkedHashMap<>();

        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            others.put(event.toString(), valueIn.object());
        }
    }

    static class ABCD extends SelfDescribingMarshallable {
        Bytes A = Bytes.allocateElasticDirect();
        Bytes B = Bytes.allocateDirect(64);
        Bytes C = Bytes.elasticByteBuffer();
        Bytes D = Bytes.allocateElasticOnHeap(1);

        void releaseAll() {
            A.releaseLast();
            B.releaseLast();
            C.releaseLast();
            D.releaseLast();
        }
    }

    static class ABC extends SelfDescribingMarshallable {
        StringBuilder A = new StringBuilder();
        StringBuilder B = new StringBuilder();
        StringBuilder C = new StringBuilder();
    }

    static class NestedA extends SelfDescribingMarshallable {
        NestedB b;
        long value;
    }

    static class NestedB extends SelfDescribingMarshallable {
        double field1;
    }

    static class StringArray implements Marshallable {
        String[] strings;
    }

    static class BytesWrapper extends SelfDescribingMarshallable {
        @NotNull
        Bytes bytes = allocateElasticDirect();

        public void bytes(@NotNull CharSequence cs) {
            bytes.clear();
            bytes.append(cs);
        }
    }

    static class DoubleWrapper extends SelfDescribingMarshallable {
        double d;
        double n;

        public DoubleWrapper(double d) {
            this.d = d;
            this.n = -d;
        }
    }

    static class NestedList extends SelfDescribingMarshallable {
        String name;
        List<NestedItem> listA = new ArrayList<>();
        List<NestedItem> listB = new ArrayList<>();
        transient List<NestedItem> listA2 = new ArrayList<>();
        transient List<NestedItem> listB2 = new ArrayList<>();
        int num;

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            name = wire.read("name").text();
            wire.read("listA").sequence(listA, listA2, NestedItem::new);
            wire.read("listB").sequence(listB, listB2, NestedItem::new);
            num = wire.read("num").int32();
        }
    }

    static class NestedItem extends SelfDescribingMarshallable {
        int a;
        double b;
    }

    static class NestedWithEnumSet extends SelfDescribingMarshallable {
        List<WithEnumSet> list = new ArrayList<>();
    }

    static class WithEnumSet extends SelfDescribingMarshallable {
        String name;
        Set<TimeUnit> timeUnits = EnumSet.noneOf(TimeUnit.class);

        @UsedViaReflection
        WithEnumSet() {
        }

        public WithEnumSet(String name) {
            this.name = name;
        }

        public WithEnumSet(String name, Set<TimeUnit> timeUnits) {
            this.name = name;
            this.timeUnits = timeUnits;
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            Wires.writeMarshallable(this, wire, false);
        }
    }

    static class MyDto extends SelfDescribingMarshallable {
        List<String> strings = new ArrayList<>();

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {

            // WORKS
            //  Wires.readMarshallable(this, wire, true); //  WORKS

            // FAILS
            Wires.readMarshallable(this, wire, false);
        }
    }

    static class DtoWithBytesField extends SelfDescribingMarshallable {
        private BytesStore bytes;
        private long another;

        @Override
        public void readMarshallable(@NotNull WireIn wire) {
            if (bytes == null)
                bytes = BytesStore.nativePointer();
            wire.read(() -> "bytes").bytesSet((PointerBytesStore) bytes);
            another = (wire.read(() -> "another").int64());
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "bytes").bytes(bytes);
            wire.write(() -> "another").int64(another);
        }
    }

    static class TwoLongs extends SelfDescribingMarshallable {

        @LongConversion(HexaDecimalConverter.class)
        long hexadecimal;

        @LongConversion(HexaDecimalConverter.class)
        long hexa2;

        public TwoLongs(long hexadecimal, long hexa2) {
            this.hexadecimal = hexadecimal;
            this.hexa2 = hexa2;
        }
    }

    static class HexaDecimalConverter implements LongConverter {
        @Override
        public long parse(CharSequence text) {
            return Long.parseUnsignedLong(text.toString(), 16);
        }

        @Override
        public void append(StringBuilder text, long value) {
            text.append(Long.toHexString(value));
        }
    }

}
