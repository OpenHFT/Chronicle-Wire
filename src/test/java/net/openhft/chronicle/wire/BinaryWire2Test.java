/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.*;
import java.util.*;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.*;

/**
 * Created by peter.lawrey on 06/02/15.
 */
public class BinaryWire2Test {
    @NotNull
    Bytes bytes = nativeBytes();

    @NotNull
    private BinaryWire createWire() {
        bytes.clear();
        BinaryWire wire = new BinaryWire(bytes, false, false, false, 32, "lzw");
        assert wire.startUse();
        return wire;
    }

    @Test
    public void testBool() {
        Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);

        wire.read().bool("error", Assert::assertFalse)
                .read().bool("error", Assert::assertTrue)
                .read().bool("error", Assert::assertNull);
    }

    @Test
    @Ignore("TODO FIX")
    public void testBytesStore() {
        Wire wire = createWire();
        wire.write().object(Bytes.from("Hello"));

        Bytes b = Bytes.elasticByteBuffer();
        wire.read()
                .object(b, Bytes.class);
        assertEquals("Hello", b.toString());
    }

    @Test
    public void testFloat32() {
        Wire wire = createWire();
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY);

        wire.read().float32(this, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(this, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(this, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F));
    }

    @Test
    public void testTime() {
        Wire wire = createWire();
        LocalTime now = LocalTime.now();
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);

        wire.read().time(now, Assert::assertEquals)
                .read().time(LocalTime.MAX, Assert::assertEquals)
                .read().time(LocalTime.MIN, Assert::assertEquals);
    }

    @Test
    public void testZonedDateTime() {
        Wire wire = createWire();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);

        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(max, Assert::assertEquals)
                .read().zonedDateTime(min, Assert::assertEquals);
    }

    @Test
    public void testDate() {
        Wire wire = createWire();
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
        Wire wire = createWire();
        UUID uuid = UUID.randomUUID();
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));

        wire.read().uuid(this, (o, t) -> assertEquals(uuid, t))
                .read().uuid(this, (o, t) -> assertEquals(new UUID(0, 0), t))
                .read().uuid(this, (o, t) -> assertEquals(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), t));
    }

    @Test
    public void testSequence() {
        Wire wire = createWire();
        writeMessage(wire);

        System.out.println(wire.bytes().toHexString());

        Wire twire = new TextWire(Bytes.elasticByteBuffer());
        writeMessage(twire);

        System.out.println(Wires.fromSizePrefixedBlobs(twire.bytes()));
    }

    private void writeMessage(@NotNull WireOut wire) {
        wire.writeDocument(true, w -> w
                .write("csp").text("//path/service")
                .write("tid").int64(123456789));
        wire.writeDocument(false, w -> w
                .write("entrySet").sequence(s -> {
                    s.marshallable(m -> m
                            .write("key").text("key-1")
                            .write("value").text("value-1"));
                    s.marshallable(m -> m
                            .write("key").text("key-2")
                            .write("value").text("value-2"));
                }));
    }

    @Test
    public void testSequenceContext() {
        Wire wire = createWire();
        writeMessageContext(wire);

        System.out.println(wire.bytes().toHexString());

        Wire twire = new TextWire(Bytes.elasticByteBuffer());
        writeMessageContext(twire);

        System.out.println(Wires.fromSizePrefixedBlobs(twire.bytes()));
    }

    private void writeMessageContext(@NotNull WireOut wire) {
        try (DocumentContext _ = wire.writingDocument(true)) {
            wire.write("csp").text("//path/service")
                    .write("tid").int64(123456789);
        }
        try (DocumentContext _ = wire.writingDocument(false)) {
            wire.write("entrySet").sequence(s -> {
                s.marshallable(m -> m
                        .write("key").text("key-1")
                        .write("value").text("value-1"));
                s.marshallable(m -> m
                        .write("key").text("key-2")
                        .write("value").text("value-2"));
            });
        }
    }

    @Test
    public void testEnum() {
        Wire wire = createWire();
        wire.write().object(WireType.BINARY)
                .write().object(WireType.TEXT)
                .write().object(WireType.RAW);

        assertEquals(WireType.BINARY, wire.read()
                .object(Object.class));
        assertEquals(WireType.TEXT, wire.read().object(Object.class));
        assertEquals(WireType.RAW, wire.read().object(Object.class));
    }

    @Test
    public void fieldAfterText() {
        Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("data")
                .typePrefix("!UpdateEvent").marshallable(
                        v -> v.write("assetName").text("/name")
                                .write("key").object("test")
                                .write("oldValue").object("world1")
                                .write("value").object("world2")));

        assertEquals("--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: world1,\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, w -> w.read(() -> "data").typePrefix(this, (o, t) -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                m -> m.read(() -> "assetName").object(String.class, "/name", Assert::assertEquals)
                        .read(() -> "key").object(String.class, "test", Assert::assertEquals)
                        .read(() -> "oldValue").object(String.class, "world1", Assert::assertEquals)
                        .read(() -> "value").object(String.class, "world2", Assert::assertEquals)));
    }

    @Test
    public void fieldAfterNull() {
        Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("data").typedMarshallable("!UpdateEvent",
                v -> v.write("assetName").text("/name")
                        .write("key").object("test")
                        .write("oldValue").object(null)
                        .write("value").object("world2")));

        assertEquals("--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: !!null \"\",\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, w -> w.read(() -> "data").typePrefix(this, (o, t) -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                m -> m.read(() -> "assetName").object(String.class, "/name", Assert::assertEquals)
                        .read(() -> "key").object(String.class, "test", Assert::assertEquals)
                        .read(() -> "oldValue").object(String.class, "error", Assert::assertNull)
                        .read(() -> "value").object(String.class, "world2", Assert::assertEquals)));
    }

    @Test
    public void fieldAfterNullContext() {
        Wire wire = createWire();
        try (DocumentContext _ = wire.writingDocument(true)) {
            wire.write("tid").int64(1234567890L);
        }

        try (DocumentContext _ = wire.writingDocument(false)) {
            wire.write("data").typedMarshallable("!UpdateEvent",
                    v -> v.write("assetName").text("/name")
                            .write("key").object("test")
                            .write("oldValue").object(null)
                            .write("value").object("world2"));
        }

        assertEquals("--- !!meta-data #binary\n" +
                "tid: !int 1234567890\n" +
                "# position: 13\n" +
                "--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: !!null \"\",\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent());
            assertTrue(context.isMetaData());
            Assert.assertEquals(1234567890L, wire.read(() -> "tid").int64());
        }
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent());
            assertTrue(context.isData());
            wire.read(() -> "data").typePrefix(this, (o, t) -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                    m -> m.read(() -> "assetName").object(String.class, "/name", Assert::assertEquals)
                            .read(() -> "key").object(String.class, "test", Assert::assertEquals)
                            .read(() -> "oldValue").object(String.class, "error", Assert::assertNull)
                            .read(() -> "value").object(String.class, "world2", Assert::assertEquals));
        }
        try (DocumentContext context = wire.readingDocument()) {
            assertFalse(context.isPresent());
        }
    }

    @Test
    public void readDemarshallable() {
        Wire wire = createWire();
        try (DocumentContext $ = wire.writingDocument(true)) {
            wire.getValueOut().typedMarshallable(new DemarshallableObject("test", 123456));
        }

        assertEquals("--- !!meta-data #binary\n" +
                "!net.openhft.chronicle.wire.DemarshallableObject {\n" +
                "  name: test,\n" +
                "  value: !int 123456\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));

        try (DocumentContext $ = wire.readingDocument()) {
            DemarshallableObject dobj = wire.getValueIn().typedMarshallable();
            assertEquals("test", dobj.name);
            assertEquals(123456, dobj.value);
        }
    }

    @Test
    public void testSnappyCompressWithSnappy() throws IOException {
        Wire wire = createWire();
        String str = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        wire.write("message").compress("snappy", str);

        wire.bytes().readPositionUnlimited(0);
        String str2 = wire.read(() -> "message").text();
        assertEquals(str, str2);

        wire.bytes().readPositionUnlimited(0);
        Bytes asText = Bytes.elasticByteBuffer();
        wire.copyTo(new TextWire(asText));
        assertEquals("message: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n", asText.toString());
    }

    @Test
    @Ignore("Todo fix")
    public void testSnappyCompressWithSnappy2() throws IOException {
        Wire wire = createWire();
        Bytes str = Bytes.from("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        wire.write("message").compress("snappy", str);

        wire.bytes().readPosition(0);
        String str2 = wire.read(() -> "message").text();
        assertEquals(str.toString(), str2);

        wire.bytes().readPosition(0);
        Bytes asText = Bytes.elasticByteBuffer();
        wire.copyTo(new TextWire(asText));
        assertEquals("message: # snappy\n" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n", asText.toString());
    }

    @Ignore
    @Test
    public void testCompression() {
        for (String comp : "binary,gzip,lzw".split(",")) {
            bytes.clear();

            Wire wire = new BinaryWire(bytes, false, false, false, 32, comp);
            assert wire.startUse();
            String str = "xxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyyyyyyyy2yyyyyyyyyyyyyyyyy";
            BytesStore bytes = BytesStore.from(str);

            wire.write().bytes(bytes);
            System.out.println(comp + ": str.length() = " + str.length() + ", wire.bytes().readRemaining() = " + wire.bytes().readRemaining());
            if (!comp.equals("binary"))
                assertTrue(wire.bytes().readRemaining() + " >= " + str.length(),
                        wire.bytes().readRemaining() < str.length());

            wire.bytes().readPositionUnlimited(0);
            BytesStore bytesStore = wire.read()
                    .bytesStore();
            assert bytesStore != null;
            assertEquals(bytes.toDebugString(), bytesStore.toDebugString());
        }
    }

    @Test
    public void testByteArrayValueWithRealBytesNegative() {
        Wire wire = createWire();

        final byte[] expected = {-1, -2, -3, -4, -5, -6, -7};
        wire.writeDocument(false, wir -> wir.writeEventName(() -> "put")
                .marshallable(w -> w.write("key").text("1")
                        .write("value")
                        .object(expected)));
        System.out.println(wire);

        wire.readDocument(null, wir -> wire.read(() -> "put")
                .marshallable(w -> w.read(() -> "key").object(Object.class, "1", Assert::assertEquals)
                        .read(() -> "value").object(Object.class, expected, (e, v) -> {
                            Assert.assertArrayEquals(e, (byte[]) v);
                        })));
    }

    @Test
    public void testBytesArray() {
        Wire wire = createWire();
        Random rand = new Random();
        for (int i = 0; i < 70000; i += rand.nextInt(i + 1) + 1) {
            System.out.println(i);
            wire.clear();
            final byte[] fromBytes = new byte[i];
            wire.writeDocument(false, w -> w.write("bytes").bytes(fromBytes));
            Wires.fromSizePrefixedBlobs(wire);
            int finalI = i;
            wire.readDocument(null, w -> assertEquals(finalI, w.read(() -> "bytes").bytes().length));
        }
    }

    @Test
    public void testSmallArray() {
        Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("index")
                .int64array(10));
        assertEquals("--- !!data #binary\n" +
                "index: [\n" +
                "  # length: 10, used: 0\n" +
                "  0, 0, 0, 0, 0, 0, 0, 0, 0, 0\n" +
                "]\n", Wires.fromSizePrefixedBlobs(wire.bytes()));
    }

    @Test
    public void testTypeLiteral() {
        Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("a").typeLiteral(String.class)
                .write("b").typeLiteral(int.class)
                .write("c").typeLiteral(byte[].class)
                .write("d").typeLiteral(Double[].class)
                .write("z").typeLiteral((Class) null));
        assertEquals("--- !!data #binary\n" +
                "a: !type String\n" +
                "b: !type int\n" +
                "c: !type \"byte[]\"\n" +
                "d: !type \"[Ljava.lang.Double;\"\n" +
                "z: !!null \"\"\n", Wires.fromSizePrefixedBlobs(wire.bytes()));
    }

    @Test
    public void testByteArray() {
        Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("nothing").object(new byte[0]));
        byte[] one = {1};
        wire.writeDocument(false, w -> w.write("one").object(one));
        byte[] four = {1, 2, 3, 4};
        wire.writeDocument(false, w -> w.write("four").object(four));

        assertEquals("--- !!data #binary\n" +
                        "nothing: !byte[] \"\"\n" +
                        "# position: 23\n" +
                        "--- !!data #binary\n" +
                        "one: !byte[] !!binary AQ==\n" +
                        "\n" +
                        "# position: 43\n" +
                        "--- !!data #binary\n" +
                        "four: !byte[] !!binary AQIDBA==\n" +
                        "\n"
                , Wires.fromSizePrefixedBlobs(wire.bytes()));
        wire.readDocument(null, w -> assertArrayEquals(new byte[0], (byte[]) w.read(() -> "nothing").object()));
        wire.readDocument(null, w -> assertArrayEquals(one, (byte[]) w.read(() -> "one").object()));
        wire.readDocument(null, w -> assertArrayEquals(four, (byte[]) w.read(() -> "four").object()));
    }

    @Test
    public void testObjectKeys() {
        Map<MyMarshallable, String> map = new LinkedHashMap<>();
        map.put(new MyMarshallable("key1"), "value1");
        map.put(new MyMarshallable("key2"), "value2");

        Wire wire = createWire();
        final MyMarshallable parent = new MyMarshallable("parent");
        wire.writeDocument(false, w -> w.writeEvent(MyMarshallable.class, parent).object(map));

        assertEquals("--- !!data #binary\n" +
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
            final Map map2 = w.getValueIn()
                    .object(Map.class);
            assertEquals(map, map2);
        });
    }

    @Test
    public void testBytesLiteral() {
        Wire wire = new BinaryWire(Bytes.elasticByteBuffer());
        wire.write("test").text("Hello World");

        final BinaryWire wire1 = createWire();
        wire1.writeDocument(false, (WireOut w) -> w.write(() -> "nested")
                .bytesLiteral(wire.bytes()));

        assertEquals("--- !!data #binary\n" +
                "nested: {\n" +
                "  test: Hello World\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire1));

        wire1.readDocument(null, w -> {
            final BytesStore bytesStore = w.read(() -> "nested")
                    .bytesLiteral();
            assertEquals(wire.bytes(), bytesStore);
        });
    }

    @Test
    public void testWriteMap() {
        Wire wire = new BinaryWire(Bytes.elasticByteBuffer());

        Map<String, Object> putMap = new HashMap<String, Object>();
        putMap.put("TestKey", "TestValue");
        putMap.put("TestKey2", 1.0);

        wire.writeAllAsMap(String.class, Object.class, putMap);

        Map<String, Object> newMap = new HashMap<String, Object>();

        wire.readAllAsMap(String.class, Object.class, newMap);

        Assert.assertEquals(putMap, newMap);

    }
}

