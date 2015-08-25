/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.NoBytesStore;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xerial.snappy.Snappy;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class TextWireTest {

    Bytes bytes;

    @Test
    public void testWrite() {
        Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        assertEquals("\"\": \"\": \"\": ", wire.toString());
    }

    @NotNull
    private TextWire createWire() {
        bytes = nativeBytes();
        return new TextWire(bytes);
    }

    @Test
    public void testSimpleBool() {
        Wire wire = createWire();

        wire.write(() -> "F").bool(false);
        wire.write(() -> "T").bool(true);
        assertEquals("F: false\n" +
                "T: true\n", wire.toString());
        String expected = "{F=false, T=true}";
        expectWithSnakeYaml(expected, wire);

        assertEquals(false, wire.read(() -> "F").bool());
        assertEquals(true, wire.read(() -> "T").bool());
    }

    @Test
    public void testLeadingSpace() {
        Wire wire = createWire();
        wire.write().text(" leadingspace");
        assertEquals(" leadingspace", wire.read().text());
    }

    private void expectWithSnakeYaml(String expected, @NotNull Wire wire) {
        String s = wire.toString();
        Object load = null;
        try {
            Yaml yaml = new Yaml();
            load = yaml.load(new StringReader(s));
        } catch (Exception e) {
            System.out.println(s);
            throw e;
        }
        assertEquals(expected, load.toString());
    }

    @Test
    public void testInt64() {
        Wire wire = createWire();
        long expected = 1234567890123456789L;
        wire.write(() -> "VALUE").int64(expected);
        expectWithSnakeYaml("{VALUE=1234567890123456789}", wire);
        assertEquals(expected, wire.read(() -> "VALUE").int64());
    }

    @Test
    public void testInt16() {
        Wire wire = createWire();
        short expected = 12345;
        wire.write(() -> "VALUE").int64(expected);
        expectWithSnakeYaml("{VALUE=12345}", wire);
        assertEquals(expected, wire.read(() -> "VALUE").int16());
    }

    @Test(expected = IllegalStateException.class)
    public void testInt16TooLarge() {
        Wire wire = createWire();
        wire.write(() -> "VALUE").int64(Long.MAX_VALUE);
        wire.read(() -> "VALUE").int16();
    }

    @Test
    public void testInt32() {
        Wire wire = createWire();
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
        Wire wire = createWire();
        wire.write(() -> "VALUE").int64(Integer.MAX_VALUE);
        wire.read(() -> "VALUE").int16();
    }

    @Test
    public void testWrite1() {
        Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        assertEquals("field1: field2: field3: ", wire.toString());
    }

    @Test
    public void testWrite2() {
        Wire wire = createWire();
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");
        assertEquals("Hello: World: \"Long field name which is more than 32 characters, Bye\": ", wire.toString());
    }

    @Test
    public void testRead() {
        Wire wire = createWire();
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
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        try {
            wire.read(BWKey.field1);
            fail();
        } catch (UnsupportedOperationException expected) {
            StringBuilder name = new StringBuilder();
            wire.read(name);
            assertEquals("Test", name.toString());
        }
        assertEquals(1, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead2() {
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name1);

        // ok as blank matches anything
        StringBuilder name = new StringBuilder();
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
        Wire wire = createWire();
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);
        wire.write(() -> "Test").int8(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int8(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int16() {
        Wire wire = createWire();
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);
        wire.write(() -> "Test").int16(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int16(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint8() {
        Wire wire = createWire();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write(() -> "Test").uint8(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint8(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint16() {
        Wire wire = createWire();
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write(() -> "Test").uint16(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void uint32() {
        Wire wire = createWire();
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write(() -> "Test").uint32(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int32() {
        Wire wire = createWire();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write(() -> "Test").int32(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int64() {
        Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);
        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
        IntConsumer ic = i::set;
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void float64() {
        Wire wire = createWire();
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
        Floater n = new Floater();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n::set);
            assertEquals(e, n.f, 0.0);
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void text() {
        Wire wire = createWire();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        String name1 = "Long field name which is more than 32 characters, \\ \nBye";

        wire.write(() -> "Test")
                .text(name1);
        expectWithSnakeYaml("{=Hello, field1=world, Test=Long field name which is more than 32 characters, \\ \n" +
                "Bye}", wire);
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, \\\\ \\nBye\"\n", wire.toString());

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name1).forEach(e -> {
            assertNotNull(wire.read().textTo(sb));
            assertEquals(e, sb.toString());
        });

        assertEquals(1, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void type() {
        Wire wire = createWire();
        wire.write().type("MyType");
        wire.write(BWKey.field1).type("AlsoMyType");
        String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write(() -> "Test").type(name1);
        wire.writeComment("");
        // TODO fix how types are serialized.
//        expectWithSnakeYaml(wire, "{=1, field1=2, Test=3}");
        assertEquals("\"\": !MyType " +
                "field1: !AlsoMyType " +
                "Test: !" + name1 + " # \n", wire.toString());

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().type(sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(3, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testBool() {
        Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);
        wire.read().bool(Assert::assertFalse)
                .read().bool(Assert::assertTrue)
                .read().bool(Assert::assertNull);
    }

    @Test
    public void testFloat32() {
        Wire wire = createWire();
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY)
                .write().float32(Float.NEGATIVE_INFINITY)
                .write().float32(123456.0f);
        System.out.println(wire.bytes());
        wire.read().float32(t -> assertEquals(0.0F, t, 0.0F))
                .read().float32(t -> assertTrue(Float.isNaN(t)))
                .read().float32(t -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F))
                .read().float32(t -> assertEquals(Float.NEGATIVE_INFINITY, t, 0.0F))
                .read().float32(t -> assertEquals(123456.0f, t, 0.0F));
    }

    @Test
    public void testTime() {
        Wire wire = createWire();
        LocalTime now = LocalTime.now();
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);
        assertEquals("\"\": " + now + "\n" +
                        "\"\": 23:59:59.999999999\n" +
                        "\"\": 00:00\n",
                bytes.toString());
        wire.read().time(t -> assertEquals(now, t))
                .read().time(t -> assertEquals(LocalTime.MAX, t))
                .read().time(t -> assertEquals(LocalTime.MIN, t));
    }

    @Test
    public void testZonedDateTime() {
        Wire wire = createWire();
        ZonedDateTime now = ZonedDateTime.now();
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()))
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()));
        wire.read().zonedDateTime(t -> assertEquals(now, t))
                .read().zonedDateTime(t -> assertEquals(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()), t))
                .read().zonedDateTime(t -> assertEquals(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()), t));
    }

    @Test
    public void testDate() {
        Wire wire = createWire();
        LocalDate now = LocalDate.now();
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);
        wire.read().date(t -> assertEquals(now, t))
                .read().date(t -> assertEquals(LocalDate.MAX, t))
                .read().date(t -> assertEquals(LocalDate.MIN, t));
    }

    @Test
    public void testUuid() {
        Wire wire = createWire();
        UUID uuid = UUID.randomUUID();
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));
        wire.read().uuid(t -> assertEquals(uuid, t))
                .read().uuid(t -> assertEquals(new UUID(0, 0), t))
                .read().uuid(t -> assertEquals(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), t));
    }

    @Test
    public void testBytes() {
        Wire wire = createWire();
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;
        wire.write().bytes(NoBytesStore.NO_BYTES)
                .write().bytes(Bytes.wrapForRead("Hello".getBytes()))
                .write().bytes(Bytes.wrapForRead("quotable, text".getBytes()))
                .write().bytes(allBytes);
        System.out.println(bytes.toString());
        NativeBytes allBytes2 = nativeBytes();
        wire.read().bytes(wi -> assertEquals(0, wi.bytes().readRemaining()))
                .read().bytes(wi -> assertEquals("Hello", wi.bytes().toString()))
                .read().bytes(wi -> assertEquals("quotable, text", wi.bytes().toString()))
                .read().bytes(allBytes2);
        assertEquals(Bytes.wrapForRead(allBytes), allBytes2);
    }

    @Test
    public void testWriteMarshallable() {
        Wire wire = createWire();
        MyTypes mtA = new MyTypes();
        mtA.b(true);
        mtA.d(123.456);
        mtA.i(-12345789);
        mtA.s((short) 12345);
        mtA.text.append("Hello World");

        wire.write(() -> "A").marshallable(mtA);

        MyTypes mtB = new MyTypes();
        mtB.b(false);
        mtB.d(123.4567);
        mtB.i(-123457890);
        mtB.s((short) 1234);
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

        MyTypes mt2 = new MyTypes();
        wire.read(() -> "A").marshallable(mt2);
        assertEquals(mt2, mtA);

        wire.read(() -> "B").marshallable(mt2);
        assertEquals(mt2, mtB);
    }

    @Test
    public void testWriteMarshallableAndFieldLength() {
        Wire wire = createWire();
        MyTypes mtA = new MyTypes();
        mtA.b(true);
        mtA.d(123.456);
        mtA.i(-12345789);
        mtA.s((short) 12345);

        ValueOut write = wire.write(() -> "A");

        long start = wire.bytes().writePosition() + 1; // including one space for "sep".
        write.marshallable(mtA);
        long fieldLen = wire.bytes().writePosition() - start;

        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=}}", wire);

        ValueIn read = wire.read(() -> "A");

        long len = read.readLength();

        assertEquals(fieldLen, len, 1);
    }

    @Test
    public void testMapReadAndWriteStrings() {
        final Bytes bytes = nativeBytes();
        final Wire wire = new TextWire(bytes);

        final Map<String, String> expected = new LinkedHashMap<>();

        expected.put("hello", "world");
        expected.put("hello1", "world1");
        expected.put("hello2", "world2");

        wire.writeDocument(false, o -> {
            o.write(() -> "example")
                    .map(expected);
        });
//        bytes.readPosition(4);
//        expectWithSnakeYaml("{example={hello=world, hello1=world1, hello2=world2}}", wire);
//        bytes.readPosition(0);
        assertEquals("--- !!data\n" +
                        "example: !!seqmap [\n" +
                        "  { key: hello,\n" +
                        "    value: world },\n" +
                        "  { key: hello1,\n" +
                        "    value: world1 },\n" +
                        "  { key: hello2,\n" +
                        "    value: world2 }\n" +
                        "]\n",
                Wires.fromSizePrefixedBlobs(bytes));
        final Map<String, String> actual = new LinkedHashMap<>();
        wire.readDocument(null, c -> c.read(() -> "example").map(actual));
        assertEquals(actual, expected);
    }

    @Test
    @Ignore
    public void testMapReadAndWriteIntegers() {
        final Bytes bytes = nativeBytes();
        final Wire wire = new TextWire(bytes);

        final Map<Integer, Integer> expected = new HashMap<>();

        expected.put(1, 2);
        expected.put(2, 2);
        expected.put(3, 3);

        wire.writeDocument(false, o -> {
            o.write(() -> "example").map(expected);
        });

        expectWithSnakeYaml("{=1, field1=2, Test=3}", wire);

        assertEquals("--- !!data\n" +
                "example: !!map {" +
                "  ? !!int 1\n" +
                "  : !! int 1\n" +
                "  ? !!int 2\n" +
                "  : !!int 2\n" +
                "  ? !!int 3:\n" +
                "  : !!int 3\n" +
                "}\n", Wires.fromSizePrefixedBlobs(bytes));
        final Map<Integer, Integer> actual = new HashMap<>();
        wire.readDocument(null, c -> {
            Map m = c.read(() -> "example").map(Integer.class, Integer.class, actual);
            assertEquals(m, expected);
        });
    }

    @Test
    public void testMapReadAndWriteMarshable() {
        final Bytes bytes = nativeBytes();
        final Wire wire = new TextWire(bytes);

        final Map<Marshallable, Marshallable> expected = new LinkedHashMap<>();

        expected.put(new MyMarshallable("aKey"), new MyMarshallable("aValue"));
        expected.put(new MyMarshallable("aKey2"), new MyMarshallable("aValue2"));

        wire.writeDocument(false, o -> o.write(() -> "example").map(expected));

        assertEquals("--- !!data\n" +
                "example: !!seqmap [\n" +
                "  { key: !net.openhft.chronicle.wire.TextWireTest$MyMarshallable { MyField: aKey },\n" +
                "    value: !net.openhft.chronicle.wire.TextWireTest$MyMarshallable { MyField: aValue } },\n" +
                "  { key: !net.openhft.chronicle.wire.TextWireTest$MyMarshallable { MyField: aKey2 },\n" +
                "    value: !net.openhft.chronicle.wire.TextWireTest$MyMarshallable { MyField: aValue2 } }\n" +
                "]\n", Wires.fromSizePrefixedBlobs(bytes));
        final Map<MyMarshallable, MyMarshallable> actual = new LinkedHashMap<>();

        wire.readDocument(null, c -> c.read(() -> "example")
                .map(
                        MyMarshallable.class,
                        MyMarshallable.class,
                        actual));

        assertEquals(expected, actual);
    }

    @Test
    public void testException() {
        Exception e = new InvalidAlgorithmParameterException("Reference cannot be null") {
            @NotNull
            @Override
            public StackTraceElement[] getStackTrace() {
                StackTraceElement[] stack = {
                        new StackTraceElement("net.openhft.chronicle.wire.TextWireTest", "testException", "TextWireTest.java", 783),
                        new StackTraceElement("net.openhft.chronicle.wire.TextWireTest", "runTestException", "TextWireTest.java", 73),
                        new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke0", "NativeMethodAccessorImpl.java", -2)
                };
                return stack;
            }
        };
        final Bytes bytes = nativeBytes();
        final Wire wire = new TextWire(bytes);
        wire.writeDocument(false, w -> w.writeEventName(() -> "exception").object(e));

        // TODO figure out where the newline before ] went.
        assertEquals("--- !!data\n" +
                "exception: !" + e.getClass().getName() + " {\n" +
                "  message: Reference cannot be null,\n" +
                "  stackTrace: [\n" +
                "    { class: net.openhft.chronicle.wire.TextWireTest, method: testException, file: TextWireTest.java, line: 783 },\n" +
                "    { class: net.openhft.chronicle.wire.TextWireTest, method: runTestException, file: TextWireTest.java, line: 73 }\n" +
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

        Wire wire = createWire();
        wire.write().object(WireType.BINARY)
                .write().object(WireType.TEXT)
                .write().object(WireType.RAW);

        assertEquals("\"\": !WireType BINARY\n" +
                "\"\": !WireType TEXT\n" +
                "\"\": !WireType RAW\n", bytes.toString());

        assertEquals(WireType.BINARY, wire.read().object(Object.class));
        assertEquals(WireType.TEXT, wire.read().object(Object.class));
        assertEquals(WireType.RAW, wire.read().object(Object.class));
    }

    @Test
    public void testArrays() {
        Wire wire = createWire();

        Object[] noObjects = {};
        wire.write().object(noObjects);

        Object[] object = wire.read().object(Object[].class);
        assertEquals(0, object.length);

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write().object(threeObjects);

        Object[] object2 = wire.read()
                .object(Object[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    @Test
    public void testSnappyCompression() throws IOException {
        Wire wire = createWire();
        String str = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";


        byte[] compressedBytes = Snappy.compress(str.getBytes());
        wire.write().snappy(compressedBytes);

        byte[] returnBytes = wire.read().snappy();
        assertArrayEquals(compressedBytes, returnBytes);
    }

    @Test
    public void testSnappyCompressionAsText() throws IOException {
        Wire wire = createWire();
        String str = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";


        byte[] compressedBytes = Snappy.compress(str.getBytes());
        wire.write().snappy(compressedBytes);

        String returnString = wire.read().text();
        assertEquals(str, returnString);
    }

    @Test
    public void testSnappyCompressWithSnappy() throws IOException {
        Wire wire = createWire();
        String str = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        wire.write().compressWithSnappy(str);

        String returnString = wire.read().text();
        assertEquals(str, returnString);
    }

    @Test
    public void testStringArrays() {
        Wire wire = createWire();

        String[] noObjects = {};
        wire.write().object(noObjects);

        String[] object = wire.read().object(String[].class);
        assertEquals(0, object.length);

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        String[] threeObjects = {"abc", "def", "ghi"};
        wire.write().object(threeObjects);

        String[] object2 = wire.read()
                .object(String[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    @Test
    public void testStringList() {
        Wire wire = createWire();

        List<String> noObjects = new ArrayList();
        wire.write().object(noObjects);

        List<String> list = wire.read().object(List.class);
        assertEquals(0, list.size());

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        List<String> threeObjects = Arrays.asList(new String[]{"abc", "def", "ghi"});
        wire.write().object(threeObjects);

        List<String> list2 = wire.read()
                .object(List.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }

    @Test
    public void testStringSet() {
        Wire wire = createWire();

        Set<String> noObjects = new HashSet();
        wire.write().object(noObjects);

        Set<String> list = wire.read().object(Set.class);
        assertEquals(0, list.size());

        // TODO we shouldn't need to create a new wire.
        wire = createWire();

        Set<String> threeObjects = new HashSet(Arrays.asList(new String[]{"abc", "def", "ghi"}));
        wire.write().object(threeObjects);

        Set<String> list2 = wire.read()
                .object(Set.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }

    @Test
    @Ignore
    public void testStringMap() {
        Wire wire = createWire();

        Map<String, String> noObjects = new HashMap();
        wire.write().object(noObjects);

        Map<String, String> map = wire.read().object(Map.class);
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
        String s = "cluster: {\n" +
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
        BiConsumer<String, Integer> results = EasyMock.createMock(BiConsumer.class);
        results.accept("host1", 1);
        results.accept("host2", 2);
        results.accept("host4", 4);
        replay(results);
        TextWire wire = TextWire.from(s);
        wire.read(() -> "cluster").marshallable(v -> {
                    StringBuilder sb = new StringBuilder();
                    while (wire.hasMore()) {
                        wire.readEventName(sb).marshallable(m -> {
                            m.read(() -> "hostId").int32(i -> results.accept(sb.toString(), i));
                        });
                    }
                }
        );
        verify(results);
    }

    enum BWKey implements WireKey {
        field1, field2, field3

    }

    class MyMarshallable implements Marshallable {

        @Nullable
        String someData;

        public MyMarshallable(String someData) {
            this.someData = someData;
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "MyField").text(someData);
        }

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
            someData = wire.read(() -> "MyField").text();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyMarshallable that = (MyMarshallable) o;

            return !(someData != null ? !someData.equals(that.someData) : that.someData != null);

        }

        @Override
        public int hashCode() {
            return someData != null ? someData.hashCode() : 0;
        }

        @NotNull
        @Override
        public String toString() {
            return "MyMarshable{" + "someData='" + someData + '\'' + '}';
        }

    }

}
