package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.NoBytesStore;
import org.junit.Assert;
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

public class TextWireTest {

    Bytes bytes;

    private TextWire createWire() {
        bytes = nativeBytes();
        return new TextWire(bytes);
    }

    enum BWKey implements WireKey {
        field1, field2, field3
    }

    @Test
    public void testWrite() {
        Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        wire.flip();
        assertEquals("\"\": \"\": \"\": ", wire.toString());
    }

    @Test
    public void testWrite1() {
        Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        wire.flip();
        assertEquals("field1: field2: field3: ", wire.toString());
    }

    @Test
    public void testWrite2() {
        Wire wire = createWire();
        wire.write("Hello", BWKey.field1);
        wire.write("World", BWKey.field2);
        wire.write("Long field name which is more than 32 characters, Bye", BWKey.field3);
        wire.flip();
        assertEquals("Hello: World: \"Long field name which is more than 32 characters, Bye\": ", wire.toString());
    }

    @Test
    public void testRead() {
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write("Test", BWKey.field2);
        wire.flip();
        wire.read();
        wire.read();
        wire.read();
        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead1() {
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        wire.write("Test", BWKey.field2);
        wire.flip();

        // ok as blank matches anything
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        // not a match
        try {
            wire.read(BWKey.field1);
            fail();
        } catch (UnsupportedOperationException expected) {
            StringBuilder name = new StringBuilder();
            wire.read(name, BWKey.field1);
            assertEquals("Test", name.toString());
        }
        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void testRead2() {
        Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(name1, BWKey.field3);
        wire.flip();

        // ok as blank matches anything
        StringBuilder name = new StringBuilder();
        wire.read(name, BWKey.field1);
        assertEquals(0, name.length());

        wire.read(name, BWKey.field1);
        assertEquals(BWKey.field1.name(), name.toString());

        wire.read(name, BWKey.field1);
        assertEquals(name1, name.toString());

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void int8() {
        Wire wire = createWire();
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);
        wire.write("Test", BWKey.field2).int8(3);
        wire.flip();
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int8(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void int16() {
        Wire wire = createWire();
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);
        wire.write("Test", BWKey.field2).int16(3);
        wire.flip();
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int16(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void uint8() {
        Wire wire = createWire();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);
        wire.write("Test", BWKey.field2).uint8(3);
        wire.flip();
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint8(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void uint16() {
        Wire wire = createWire();
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);
        wire.write("Test", BWKey.field2).uint16(3);
        wire.flip();
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void uint32() {
        Wire wire = createWire();
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);
        wire.write("Test", BWKey.field2).uint32(3);
        wire.flip();
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void int32() {
        Wire wire = createWire();
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);
        wire.write("Test", BWKey.field2).int32(3);
        wire.flip();
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

        // ok as blank matches anything
        AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i::set);
            assertEquals(e, i.get());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void int64() {
        Wire wire = createWire();
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write("Test", BWKey.field2).int64(3);
        wire.flip();
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

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();

    }

    @Test
    public void float64() {
        Wire wire = createWire();
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write("Test", BWKey.field2).float64(3);
        wire.flip();
        assertEquals("\"\": 1\n" +
                "field1: 2\n" +
                "Test: 3\n", wire.toString());

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

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void text() {
        Wire wire = createWire();
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        String name1 = "Long field name which is more than 32 characters, \\ \nBye";

        wire.write("Test", BWKey.field2)
                .text(name1);
        wire.flip();
        assertEquals("\"\": Hello\n" +
                "field1: world\n" +
                "Test: \"Long field name which is more than 32 characters, \\\\ \\nBye\"\n", wire.toString());

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name1).forEach(e -> {
            wire.read()
                    .text(sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(0, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }

    @Test
    public void type() {
        Wire wire = createWire();
        wire.write().type("MyType");
        wire.write(BWKey.field1).type("AlsoMyType");
        String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";
        wire.write("Test", BWKey.field2).type(name1);
        wire.writeComment("");
        wire.flip();
        assertEquals("\"\": !MyType " +
                "field1: !AlsoMyType " +
                "Test: !" + name1 + " # \n", wire.toString());

        // ok as blank matches anything
        StringBuilder sb = new StringBuilder();
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().type(sb);
            assertEquals(e, sb.toString());
        });

        assertEquals(3, bytes.remaining());
        // check it's safe to read too much.
        wire.read();
    }


    @Test
    public void testBool() {
        Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);
        wire.flip();
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
        wire.flip();
        System.out.println(wire.bytes());
        wire.read().float32(t -> assertEquals(0.0F, t, 0.0F))
                .read().float32(Float::isNaN)
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
        wire.flip();
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
        wire.flip();
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
        wire.flip();
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
        wire.flip();
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
                .write().bytes(Bytes.wrap("Hello".getBytes()))
                .write().bytes(allBytes);
        wire.flip();
        System.out.println(bytes.toString());
        NativeBytes allBytes2 = nativeBytes();
        wire.read()
                .bytes(b -> assertEquals(0, b.length))
                .read()
                .bytes(b -> assertArrayEquals("Hello".getBytes(), b))
                .read()
                .bytes(allBytes2);
        allBytes2.flip();
        assertEquals(Bytes.wrap(allBytes), allBytes2);

    }
}