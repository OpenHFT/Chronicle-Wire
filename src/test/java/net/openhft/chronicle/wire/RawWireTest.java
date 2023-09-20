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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.*;

public class RawWireTest extends WireTestCommon {

    // Suppressing raw type warnings for the Bytes<?> object.
    @SuppressWarnings("rawtypes")
    @NotNull
    // Bytes object used to simulate wire data storage.
    Bytes<?> bytes = nativeBytes();

    // Override the method from WireTestCommon to ensure byte references are released.
    @Override
    public void assertReferencesReleased() {
        // Release the last reference held by bytes.
        bytes.releaseLast();

        // Call the superclass implementation.
        super.assertReferencesReleased();
    }

    // Test to verify the write operation on the wire without any specific data.
    @Test
    public void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        assertEquals("", wire.toString());
    }

    // Helper method to create an instance of RawWire.
    @NotNull
    private RawWire createWire() {
        bytes.clear();
        return new RawWire(bytes);
    }

    // Test to verify the write operation on the wire using BWKey fields.
    @Test
    public void testWrite1() {
        @NotNull Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        assertEquals("", wire.toString());
    }

    // Test to verify the write operation on the wire with custom field names.
    @Test
    public void testWrite2() {
        @NotNull Wire wire = createWire();
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");

        assertEquals("", wire.toString());
    }

    // Test to verify the read operation on the wire after writing some data.
    @Test
    public void testRead() {
        @NotNull Wire wire = createWire();
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

    // Test to verify reading specific fields from the wire after writing some data.
    @Test
    public void testRead1() {
        @NotNull Wire wire = createWire();
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

    // Test to verify reading specific fields from the wire after writing some data with a long name.
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
        assertEquals(0, name.length());

        wire.read(name);
        assertEquals(0, name.length());

        assertEquals(0, bytes.readRemaining());
        // check it's safe to read too much.
        wire.read();
    }

    // Test for writing and reading 8-bit integers to and from the wire.
    @Test
    public void int8() {
        @NotNull Wire wire = createWire();
        wire.write().int8(1);
        wire.write(BWKey.field1).int8(2);

        wire.write(() -> "Test").int8(3);
        assertEquals("[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

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

    // Test for writing and reading 16-bit integers to and from the wire.
    @Test
    public void int16() {
        @NotNull Wire wire = createWire();
        wire.write().int16(1);
        wire.write(BWKey.field1).int16(2);

        wire.write(() -> "Test").int16(3);
        assertEquals("[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠⒉٠⒊٠‡٠٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

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

    // Test for writing and reading 8-bit unsigned integers to and from the wire.
    @Test
    public void uint8() {
        @NotNull Wire wire = createWire();
        wire.write().uint8(1);
        wire.write(BWKey.field1).uint8(2);

        wire.write(() -> "Test").uint8(3);
        assertEquals("[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

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

    // Test case for writing and reading unsigned 16-bit integers using a Wire
    @Test
    public void uint16() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write unsigned 16-bit integers to the wire
        wire.write().uint16(1);
        wire.write(BWKey.field1).uint16(2);

        wire.write(() -> "Test").uint16(3);

        // Verify the debug representation of the written data
        @NotNull String actual = wire.bytes().toDebugString();
        assertEquals("[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠⒉٠⒊٠‡٠٠٠٠٠٠٠٠٠٠", actual);

        // ok as blank matches anything
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint16(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining());

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading unsigned 32-bit integers using a Wire
    @Test
    public void uint32() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write unsigned 32-bit integers to the wire
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);

        wire.write(() -> "Test").uint32(3);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 12, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠٠٠⒉٠٠٠⒊٠٠٠‡٠٠٠٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

        // Read the unsigned 32-bit integers from the wire
        @NotNull AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining());

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading signed 32-bit integers using a Wire
    @Test
    public void int32() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write signed 32-bit integers to the wire
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);

        wire.write(() -> "Test").int32(3);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 12, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠٠٠⒉٠٠٠⒊٠٠٠‡٠٠٠٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

        // Read the signed 32-bit integers from the wire
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i, AtomicInteger::set);
            assertEquals(e, i.get());
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining());

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading signed 64-bit integers using a Wire
    @Test
    public void int64() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write signed 64-bit integers to the wire
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);

        wire.write(() -> "Test").int64(3);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 24, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠٠٠٠٠٠٠⒉٠٠٠٠٠٠٠⒊٠٠٠٠٠٠٠‡٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

        // ok as blank matches anything
        @NotNull AtomicLong i = new AtomicLong();
        IntConsumer ic = i::set;
        assertNotNull(ic);
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get());
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining());

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading 64-bit floating-point numbers using a Wire
    @Test
    public void float64() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write 64-bit floating-point numbers to the wire
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);

        wire.write(() -> "Test").float64(3);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 24, wlim: 8EiB, cap: 8EiB ] ǁ٠٠٠٠٠٠ð?٠٠٠٠٠٠٠@٠٠٠٠٠٠⒏@‡٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

        // ok as blank matches anything
        class Floater {
            double f;

            public void set(double d) {
                f = d;
            }
        }

        // Read the 64-bit floating-point numbers from the wire
        @NotNull Floater n = new Floater();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().float64(n, Floater::set);
            assertEquals(e, n.f, 0.0);
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining());

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading textual data using a Wire
    @Test
    public void text() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write textual data to the wire
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        @NotNull String name1 = "Long field name which is more than 32 characters, \\ \nBye";
        wire.write(() -> "Test").text(name1);

        wire.write(() -> "Test")
                .text(name1);
        @NotNull String actual = wire.bytes().toDebugString();

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 69, wlim: 8EiB, cap: 8EiB ] ǁ⒌Hello⒌world8Long field name which is more than 32 characters, \\ ⒑Bye‡٠٠٠٠٠٠٠٠", actual);

        // Read the textual data from the wire
        @NotNull StringBuilder sb = new StringBuilder();
        Stream.of("Hello", "world", name1).forEach(e -> {
            assertNotNull(wire.read().textTo(sb));
            assertEquals(e, sb.toString());
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining());

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading type prefixes using a Wire
    @Test
    public void type() {
        @NotNull Wire wire = createWire();

        // Writing type prefixes to the wire
        wire.write().typePrefix("MyType");
        wire.write(BWKey.field1).typePrefix("AlsoMyType");
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";

        wire.write(() -> "Test").typePrefix(name1);

        // Write an empty comment to the wire (may be a special operation in your context)
        wire.writeComment("");

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 142, wlim: 8EiB, cap: 8EiB ] ǁ⒍MyType⒑AlsoMyType{" + name1 + "‡٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());

        // Read type prefixes from the wire and validate them
        Stream.of("MyType", "AlsoMyType", name1).forEach(e -> {
            wire.read().typePrefix(e, StringUtils::isEqual);
        });

        // Ensure no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining());

        // Confirm it's safe to read beyond available data
        wire.read();
    }

    // Test case for writing and reading boolean values using a Wire
    @Test
    public void testBool() {
        @NotNull Wire wire = createWire();

        // Writing boolean values to the wire
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);

        // Reading and validating boolean values from the wire
        wire.read().bool(false, Assert::assertEquals)
                .read().bool(true, Assert::assertEquals)
                .read().bool(null, Assert::assertEquals);
    }

    // Test case for writing and reading 32-bit floating-point numbers using a Wire
    @Test
    public void testFloat32() {
        @NotNull Wire wire = createWire();

        // Writing 32-bit floating-point numbers to the wire
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY)
                .write().float32(Float.NEGATIVE_INFINITY)
                .write().float32(123456.0f);

        // Reading and validating 32-bit floating-point numbers from the wire
        wire.read().float32(this, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(this, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(this, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(Float.NEGATIVE_INFINITY, t, 0.0F))
                .read().float32(this, (o, t) -> assertEquals(123456.0f, t, 0.0F));
    }

    // Test case for writing and reading LocalTime objects using a Wire
    @Test
    public void testTime() {
        @NotNull Wire wire = createWire();
        LocalTime now = LocalTime.now();

        // Writing LocalTime objects to the wire
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);

        // Reading and validating LocalTime objects from the wire
        wire.read().time(now, Assert::assertEquals)
                .read().time(LocalTime.MAX, Assert::assertEquals)
                .read().time(LocalTime.MIN, Assert::assertEquals);
    }

    // Test case for writing and reading ZonedDateTime objects using a Wire
    @Test
    public void testZonedDateTime() {
        @NotNull Wire wire = createWire();
        ZonedDateTime now = ZonedDateTime.now();

        // Writing ZonedDateTime objects to the wire
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()))
                .write().zonedDateTime(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()));

        // Reading and validating ZonedDateTime objects from the wire
        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()), Assert::assertEquals)
                .read().zonedDateTime(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()), Assert::assertEquals);
    }

    // Test case for writing and reading LocalDate objects using a Wire
    @Test
    public void testDate() {
        @NotNull Wire wire = createWire();
        LocalDate now = LocalDate.now();

        // Writing LocalDate objects to the wire
        wire.write().date(now).write().date(LocalDate.MAX).write().date(LocalDate.MIN);

        // Reading and validating LocalDate objects from the wire
        wire.read().date(now, Assert::assertEquals)
                .read().date(LocalDate.MAX, Assert::assertEquals)
                .read().date(LocalDate.MIN, Assert::assertEquals);
    }

    // Test case for writing and reading UUID objects using a Wire
    @Test
    public void testUuid() {
        @NotNull Wire wire = createWire();
        UUID uuid = UUID.randomUUID();

        // Writing UUID objects to the wire
        wire.write().uuid(uuid)
             .write().uuid(new UUID(0, 0))
             .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));

        // Reading and validating UUID objects from the wire
        wire.read().uuid(uuid, Assert::assertEquals)
                .read().uuid(new UUID(0, 0), Assert::assertEquals)
                .read().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), Assert::assertEquals);
    }

    // Test case for writing and reading byte arrays using a Wire
    // Currently, this test is ignored due to an UnsupportedOperationException
    @Ignore("todo fix :currently using NoBytesStore so will fail with UnsupportedOperationException")
    @SuppressWarnings("rawtypes")
    @Test
    public void testBytes() {
        @NotNull Wire wire = createWire();
        @NotNull byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;

        // Writing byte arrays to the wire
        wire.write().bytes(NoBytesStore.NO_BYTES)
                .write().bytes(Bytes.wrapForRead("Hello".getBytes(ISO_8859_1)))
                .write().bytes(Bytes.wrapForRead("quotable, text".getBytes(ISO_8859_1)))
                .write().bytes(allBytes);
       // System.out.println(bytes.toDebugString());
        @NotNull NativeBytes allBytes2 = nativeBytes();
        // Reading and validating byte arrays from the wire
        wire.read().bytes(b -> assertEquals(0, b.readRemaining()))
                .read().bytes(b -> assertEquals("Hello", b.toString()))
                .read().bytes(b -> assertEquals("quotable, text", b.toString()))
                .read().bytes(allBytes2);
        assertEquals(Bytes.wrapForRead(allBytes), allBytes2);
    }

    // Test case for writing and reading custom Marshallable objects using a Wire
    @Test
    public void testWriteMarshallable() {
        @NotNull Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = (true);
        mtA.d = (123.456);
        mtA.i = (-12345789);
        mtA.s = ((short) 12345);
        mtA.text.append("Hello World");

        // Writing MyTypesCustom objects with event names to the wire
        wire.writeEventName(() -> "A").marshallable(mtA);

        @NotNull MyTypesCustom mtB = new MyTypesCustom();
        mtB.flag = (false);
        mtB.d = (123.4567);
        mtB.i = (-123457890);
        mtB.s = ((short) 1234);
        mtB.text.append("Bye now");
        wire.writeEventName(() -> "B").marshallable(mtB);

        // Validate the debug representation of the written data
        assertEquals("[pos: 0, rlim: 78, wlim: 8EiB, cap: 8EiB ] ǁ" +
                        "⒈A#٠٠٠±90w¾\\u009F\\u001A/Ý^@٠٠٠٠٠٠٠٠C\\u009ECÿ⒒Hello World" +
                        "⒈B\\u001F٠٠٠٠Ò⒋S⒌£\\u0092:Ý^@٠٠٠٠٠٠٠٠\\u009E.¤ø⒎Bye now‡٠٠٠٠٠٠٠٠",
                wire.bytes().toDebugString());

        @NotNull MyTypesCustom mt2 = new MyTypesCustom();
        @NotNull StringBuilder key = new StringBuilder();
        // Reading and validating MyTypesCustom objects from the wire
        wire.readEventName(key).marshallable(mt2);
        assertEquals("A", key.toString());
        assertEquals(mt2, mtA);

        wire.readEventName(key).marshallable(mt2);
        assertEquals("B", key.toString());
        assertEquals(mt2, mtB);
    }

    // Enum representing keys for Wire operations
    enum BWKey implements WireKey {
        field1, field2, field3
    }
}
