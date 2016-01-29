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
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.*;
import java.util.UUID;

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
        return new BinaryWire(bytes, false, false, false, 32, "lzw");
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
    public void testBytesStore() {
        Wire wire = createWire();
        wire.write().object(Bytes.from("Hello"));

        Bytes b = Bytes.elasticByteBuffer();
        wire.read().object(b, Object.class);
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
                .write(() -> "csp").text("//path/service")
                .write(() -> "tid").int64(123456789));
        wire.writeDocument(false, w -> w
                .write(() -> "entrySet").sequence(s -> {
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-1")
                            .write(() -> "value").text("value-1"));
                    s.marshallable(m -> m
                            .write(() -> "key").text("key-2")
                            .write(() -> "value").text("value-2"));
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
            wire.write(() -> "csp").text("//path/service")
                    .write(() -> "tid").int64(123456789);
        }
        try (DocumentContext _ = wire.writingDocument(false)) {
            wire.write(() -> "entrySet").sequence(s -> {
                s.marshallable(m -> m
                        .write(() -> "key").text("key-1")
                        .write(() -> "value").text("value-1"));
                s.marshallable(m -> m
                        .write(() -> "key").text("key-2")
                        .write(() -> "value").text("value-2"));
            });
        }
    }

    @Test
    public void testEnum() {
        Wire wire = createWire();
        wire.write().object(WireType.BINARY)
                .write().object(WireType.TEXT)
                .write().object(WireType.RAW);

        assertEquals(WireType.BINARY, wire.read().object(Object.class));
        assertEquals(WireType.TEXT, wire.read().object(Object.class));
        assertEquals(WireType.RAW, wire.read().object(Object.class));
    }

    @Test
    public void fieldAfterText() {
        Wire wire = createWire();
        wire.writeDocument(false, w -> w.write(() -> "data")
                .typePrefix("!UpdateEvent").marshallable(
                        v -> v.write(() -> "assetName").text("/name")
                                .write(() -> "key").object("test")
                                .write(() -> "oldValue").object("world1")
                                .write(() -> "value").object("world2")));

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
        wire.writeDocument(false, w -> w.write(() -> "data").typedMarshallable("!UpdateEvent",
                v -> v.write(() -> "assetName").text("/name")
                        .write(() -> "key").object("test")
                        .write(() -> "oldValue").object(null)
                        .write(() -> "value").object("world2")));

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
            wire.write(() -> "tid").int64(1234567890L);
        }

        try (DocumentContext _ = wire.writingDocument(false)) {
            wire.write(() -> "data").typedMarshallable("!UpdateEvent",
                    v -> v.write(() -> "assetName").text("/name")
                            .write(() -> "key").object("test")
                            .write(() -> "oldValue").object(null)
                            .write(() -> "value").object("world2"));
        }

        assertEquals("--- !!meta-data #binary\n" +
                "tid: 1234567890\n" +
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
            wire.getValueOut().typedMarshallable(new DemarshallableObject("test", 12345));
        }

        assertEquals("--- !!meta-data #binary\n" +
                "!net.openhft.chronicle.wire.DemarshallableObject {\n" +
                "  name: test,\n" +
                "  value: !int 12345\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));

        try (DocumentContext $ = wire.readingDocument()) {
            DemarshallableObject dobj = wire.getValueIn().typedMarshallable();
            assertEquals("test", dobj.name);
            assertEquals(12345, dobj.value);
        }
    }

    @Test
    public void testSnappyCompressWithSnappy() throws IOException {
        Wire wire = createWire();
        String str = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        wire.write(() -> "message").compress("snappy", str);

        wire.bytes().readPosition(0);
        String str2 = wire.read(() -> "message").text();
        assertEquals(str, str2);

        wire.bytes().readPosition(0);
        Bytes asText = Bytes.elasticByteBuffer();
        wire.copyTo(new TextWire(asText));
        assertEquals("message: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n", asText.toString());
    }

    @Test
    @Ignore("Todo fix")
    public void testSnappyCompressWithSnappy2() throws IOException {
        Wire wire = createWire();
        Bytes str = Bytes.from("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        wire.write(() -> "message").compress("snappy", str);

        wire.bytes().readPosition(0);
        String str2 = wire.read(() -> "message").text();
        assertEquals(str.toString(), str2);

        wire.bytes().readPosition(0);
        Bytes asText = Bytes.elasticByteBuffer();
        wire.copyTo(new TextWire(asText));
        assertEquals("message: # snappy\n" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n", asText.toString());
    }

    @Test
    public void testCompression() {
        for (String comp : "binary,gzip,lzw".split(",")) {
            bytes.clear();

            Wire wire = new BinaryWire(bytes, false, false, false, 32, comp);
            String str = "xxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyyyyyyyy2yyyyyyyyyyyyyyyyy";
            BytesStore bytes = BytesStore.from(str);

            wire.write()
                    .bytes(bytes);
            System.out.println(comp + ": str.length() = " + str.length() + ", wire.bytes().readRemaining() = " + wire.bytes().readRemaining());
            if (!comp.equals("binary"))
                assertTrue(wire.bytes().readRemaining() + " >= " + str.length(),
                        wire.bytes().readRemaining() < str.length());

            wire.bytes().readPosition(0);
            BytesStore bytesStore = wire.read()
                    .bytesStore();
            assert bytesStore != null;
            assertEquals(bytes.toDebugString(), bytesStore.toDebugString());
        }
    }
}
