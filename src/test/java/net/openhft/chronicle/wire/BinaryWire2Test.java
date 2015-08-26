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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.time.*;
import java.util.UUID;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by peter.lawrey on 06/02/15.
 */
public class BinaryWire2Test {
    @NotNull
    Bytes bytes = nativeBytes();
    StringBuilder csp = new StringBuilder();
    StringBuilder key1 = new StringBuilder();
    StringBuilder key2 = new StringBuilder();
    StringBuilder value1 = new StringBuilder();
    StringBuilder value2 = new StringBuilder();
    long tid;

    @NotNull
    private BinaryWire createWire() {
        bytes.clear();
        return new BinaryWire(bytes);
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
    public void testBytesStore() {
        Wire wire = createWire();
        wire.write().object(Bytes.wrapForRead("Hello"));

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

        wire.read().float32(t -> assertEquals(0.0F, t, 0.0F))
                .read().float32(t -> assertTrue(Float.isNaN(t)))
                .read().float32(t -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F));
    }

    @Test
    public void testTime() {
        Wire wire = createWire();
        LocalTime now = LocalTime.now();
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);

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

    private void readMessage(WireIn wire) {
        wire.readDocument(in ->
                        in.read(() -> "csp").text(csp)
                                .read(() -> "tid").int64(t -> tid = t),
                in ->
                        in.read(() -> "entrySet").sequence(s -> {
                            s.marshallable(m ->
                                    m.read(() -> "key").text(key1)
                                            .read(() -> "value").text(value1));
                            s.marshallable(m ->
                                    m.read(() -> "key").text(key2)
                                            .read(() -> "value").text(value2));
                        }));
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
        wire.writeDocument(false, w -> w.write(() -> "data").type("!UpdateEvent").marshallable(
                v -> v.write(() -> "assetName").text("/name")
                        .write(() -> "key").object("test")
                        .write(() -> "oldValue").object("world1")
                        .write(() -> "value").object("world2")));
        /*
--- !!not-ready-data! #binary
reply: !UpdatedEvent {
  assetName: /name,
  key: hello,
  oldValue: world1,
  value: world2
}
         */
        assertEquals("--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: world1,\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));
        wire.readDocument(null, w -> w.read(() -> "data").type(t -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                m -> m.read(() -> "assetName").object(String.class, s -> Assert.assertEquals("/name", s))
                        .read(() -> "key").object(String.class, s -> Assert.assertEquals("test", s))
                        .read(() -> "oldValue").object(String.class, s -> Assert.assertEquals("world1", s))
                        .read(() -> "value").object(String.class, s -> Assert.assertEquals("world2", s))));
    }
    @Test
    public void fieldAfterNull() {
        Wire wire = createWire();
        wire.writeDocument(false, w -> w.write(() -> "data").type("!UpdateEvent").marshallable(
                v -> v.write(() -> "assetName").text("/name")
                        .write(() -> "key").object("test")
                        .write(() -> "oldValue").object(null)
                        .write(() -> "value").object("world2")));
        /*
--- !!not-ready-data! #binary
reply: !UpdatedEvent {
  assetName: /name,
  key: hello,
  oldValue: !!null "",
  value: world2
}
         */
        assertEquals("--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: !!null \"\",\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));
        wire.readDocument(null, w -> w.read(() -> "data").type(t -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                m -> m.read(() -> "assetName").object(String.class, s -> Assert.assertEquals("/name", s))
                        .read(() -> "key").object(String.class, s -> Assert.assertEquals("test", s))
                        .read(() -> "oldValue").object(String.class, Assert::assertNull)
                        .read(() -> "value").object(String.class, s -> Assert.assertEquals("world2", s))));
    }
}
