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
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.*;

public class RawWireTest {

    @NotNull
    Bytes bytes = nativeBytes();

    @Test
    public void testWrite() {
        Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        assertEquals("", wire.toString());
    }

    @NotNull
    private RawWire createWire() {
        bytes.clear();
        return new RawWire(bytes);
    }

    @Test
    public void testWrite1() {
        Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        assertEquals("", wire.toString());
    }

    @Test
    public void testWrite2() {
        Wire wire = createWire();
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");

        assertEquals("", wire.toString());
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
        assertEquals(0, bytes.readRemaining());
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
        wire.read(BWKey.field1);
        assertEquals(0, bytes.readRemaining());
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
        assertEquals(0, name.length());

        wire.read(name);
        assertEquals(0, name.length());

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int8() {
        Wire wire = createWire();
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);

        wire.write(() -> "Test").int8(3);
        assertEquals("[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊", wire.bytes().toDebugString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
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
        Wire wire = createWire();
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);

        wire.write(() -> "Test").int16(3);
        assertEquals("[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ⒈٠⒉٠⒊٠", wire.bytes().toDebugString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
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
        Wire wire = createWire();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);

        wire.write(() -> "Test").uint8(3);
        assertEquals("[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊", wire.bytes().toDebugString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
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
        Wire wire = createWire();
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);

        wire.write(() -> "Test").uint16(3);
        assertEquals("[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ⒈٠⒉٠⒊٠", wire.bytes().toDebugString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
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
        Wire wire = createWire();
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);

        wire.write(() -> "Test").uint32(3);
        assertEquals("[pos: 0, rlim: 12, wlim: 8EiB, cap: 8EiB ] ⒈٠٠٠⒉٠٠٠⒊٠٠٠", wire.bytes().toDebugString());

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
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
        Wire wire = createWire();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);

        wire.write(() -> "Test").int32(3);
        assertEquals("[pos: 0, rlim: 12, wlim: 8EiB, cap: 8EiB ] ⒈٠٠٠⒉٠٠٠⒊٠٠٠", wire.bytes().toDebugString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
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
        Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);

        wire.write(() -> "Test").int64(3);
        assertEquals("[pos: 0, rlim: 24, wlim: 8EiB, cap: 8EiB ] ⒈٠٠٠٠٠٠٠⒉٠٠٠٠٠٠٠⒊٠٠٠٠٠٠٠", wire.bytes().toDebugString());

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
        IntConsumer ic = i::set;
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
        Wire wire = createWire();
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);

        wire.write(() -> "Test").float64(3);
        assertEquals("[pos: 0, rlim: 24, wlim: 8EiB, cap: 8EiB ] ٠٠٠٠٠٠ð?٠٠٠٠٠٠٠@٠٠٠٠٠٠⒏@", wire.bytes().toDebugString());

        // ok as blank matches anything
        class Floater {
            double f;

            public void set(double d) {
                f = d;
            }
        }
        Floater n = new Floater();
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
        Wire wire = createWire();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        String name1 = "Long field name which is more than 32 characters, \\ \nBye";

        wire.write(() -> "Test")
                .text(name1);
        String actual = wire.bytes().toDebugString();
        assertEquals("[pos: 0, rlim: 69, wlim: 8EiB, cap: 8EiB ] ⒌Hello⒌world8Long field name which is more than 32 characters, \\ ⒑Bye", actual);

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name1).forEach(e -> {
            assertNotNull(wire.read().textTo(sb));
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.readRemaining());
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
        assertEquals("[pos: 0, rlim: 142, wlim: 8EiB, cap: 8EiB ] ⒍MyType⒑AlsoMyType{" + name1, wire.bytes().toDebugString());

        // ok as blank matches anything
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().typePrefix(e, StringUtils::isEqual);
        });

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testBool() {
        Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);
        wire.read().bool(false, Assert::assertEquals)
                .read().bool(true, Assert::assertEquals)
                .read().bool(null, Assert::assertEquals);
    }

    @Test
    public void testFloat32() {
        Wire wire = createWire();
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
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()))
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()));
        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()), Assert::assertEquals)
                .read().zonedDateTime(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()), Assert::assertEquals);
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
        wire.read().uuid(uuid, Assert::assertEquals)
                .read().uuid(new UUID(0, 0), Assert::assertEquals)
                .read().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), Assert::assertEquals);
    }

    @Ignore("todo fix :currently using NoBytesStore so will fail with UnsupportedOperationException")
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
        System.out.println(bytes.toDebugString());
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
        mtA.b = (true);
        mtA.d = (123.456);
        mtA.i = (-12345789);
        mtA.s = ((short) 12345);
        mtA.text.append("Hello World");

        wire.writeEventName(() -> "A").marshallable(mtA);

        MyTypes mtB = new MyTypes();
        mtB.b = (false);
        mtB.d = (123.4567);
        mtB.i = (-123457890);
        mtB.s = ((short) 1234);
        mtB.text.append("Bye now");
        wire.writeEventName(() -> "B").marshallable(mtB);

        assertEquals("[pos: 0, rlim: 78, wlim: 8EiB, cap: 8EiB ] " +
                        "⒈A#٠٠٠±90w¾\\u009F\\u001A/Ý^@٠٠٠٠٠٠٠٠C\\u009ECÿ⒒Hello World" +
                        "⒈B\\u001F٠٠٠٠Ò⒋S⒌£\\u0092:Ý^@٠٠٠٠٠٠٠٠\\u009E.¤ø⒎Bye now",
                wire.bytes().toDebugString());

        MyTypes mt2 = new MyTypes();
        StringBuilder key = new StringBuilder();
        wire.readEventName(key).marshallable(mt2);
        assertEquals("A", key.toString());
        assertEquals(mt2, mtA);

        wire.readEventName(key).marshallable(mt2);
        assertEquals("B", key.toString());
        assertEquals(mt2, mtB);
    }

    enum BWKey implements WireKey {
        field1, field2, field3
    }
}