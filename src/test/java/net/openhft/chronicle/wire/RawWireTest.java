/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
class RawWireTest extends WireTestCommon {

    // Suppressing raw type warnings for the Bytes<?> object.
    // Bytes object used to simulate wire data storage.
    @SuppressWarnings("rawtypes")
    @NotNull
    private final Bytes<?> bytes = nativeBytes();

    // Override the method from WireTestCommon to ensure byte references are released.
    @Override
    void assertReferencesReleased() {
        // Release the last reference held by bytes.
        bytes.releaseLast();

        // Call the superclass implementation.
        super.assertReferencesReleased();
    }

    // Test to verify the write operation on the wire without any specific data.
    @Test
    @DisplayName("Write produces no output in raw wire")
    void testWrite() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write();
        wire.write();
        assertEquals("", wire.toString(), "Wire output should be empty after write() calls");
    }

    // Helper method to create an instance of RawWire.
    @NotNull
    private RawWire createWire() {
        bytes.clear();
        return new RawWire(bytes);
    }

    // Test to verify the write operation on the wire using BWKey fields.
    @Test
    @DisplayName("Writing BWKey fields produces no output")
    void testWrite1() {
        @NotNull Wire wire = createWire();
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);
        assertEquals("", wire.toString(), "Wire output should be empty after write(BWKey) calls");
    }

    // Test to verify the write operation on the wire with custom field names.
    @Test
    @DisplayName("Writing long field names produces no output")
    void testWrite2() {
        @NotNull Wire wire = createWire();
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");

        assertEquals("", wire.toString(), "Wire output should be empty after write(name) calls");
    }

    // Test to verify the read operation on the wire after writing some data.
    @Test
    @DisplayName("Reads standard fields and consumes all bytes")
    void testRead() {
        @NotNull Wire wire = createWire();
        WireReadTestSupport.writeStandardFields(wire);
        wire.read();
        wire.read();
        wire.read();
        assertEquals(0, wire.bytes().readRemaining(), "read: remaining after 3 reads");
        wire.read();
    }

    // Test to verify reading specific fields from the wire after writing some data.
    @Test
    @DisplayName("Reads key fields and consumes all bytes")
    void testRead1() {
        @NotNull Wire wire = createWire();
        WireReadTestSupport.writeStandardFields(wire);
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        assertEquals(0, wire.bytes().readRemaining(), "read(key): remaining after 3 reads");
        wire.read();
    }

    // Test to verify reading specific fields from the wire after writing some data with a long name.
    @Test
    @DisplayName("Reads long field names in raw wire")
    void testRead2() {
        @NotNull Wire wire = createWire();
        wire.write();
        wire.write(BWKey.field1);
        @NotNull String name1 = "Long field name which is more than 32 characters, Bye";
        wire.write(() -> name1);

        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name);
        assertEquals(0, name.length(), "First name read should be empty for blank field");

        name.setLength(0);
        wire.read(name);
        assertEquals("", name.toString(), "Second name read should be empty for blank field");

        name.setLength(0);
        wire.read(name);
        assertEquals("", name.toString(), "Third name read should be empty for blank field");

        assertEquals(0, wire.bytes().readRemaining(), "read(name): remaining after 3 reads");
        wire.read();
    }

    // Test for writing and reading 8-bit integers to and from the wire.
    @Test
    @DisplayName("Int8 values round-trip in raw wire")
    void int8() {
        @NotNull Wire wire = createWire();
        WireSmallIntTestSupport.writeInt8Triplet(wire);
        WireSmallIntTestSupport.expectBinaryDebug(wire.bytes(), "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠");

        WireSmallIntTestSupport.readInt8Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "int8: no remaining bytes after read");
        wire.read();
    }

    // Test for writing and reading 16-bit integers to and from the wire.
    @Test
    @DisplayName("Int16 values round-trip in raw wire")
    void int16() {
        @NotNull Wire wire = createWire();
        WireSmallIntTestSupport.writeInt16Triplet(wire);
        WireSmallIntTestSupport.expectBinaryDebug(wire.bytes(), "[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠⒉٠⒊٠‡٠٠٠٠٠٠٠٠٠٠");

        WireSmallIntTestSupport.readInt16Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "int16: no remaining bytes after read");
        wire.read();
    }

    // Test for writing and reading 8-bit unsigned integers to and from the wire.
    @Test
    @DisplayName("Uint8 values round-trip in raw wire")
    void uint8() {
        @NotNull Wire wire = createWire();
        WireSmallIntTestSupport.writeUint8Triplet(wire);
        WireSmallIntTestSupport.expectBinaryDebug(wire.bytes(), "[pos: 0, rlim: 3, wlim: 8EiB, cap: 8EiB ] ǁ⒈⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠");

        WireSmallIntTestSupport.readUint8Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "uint8: no remaining bytes after read");
        wire.read();
    }

    // Test case for writing and reading unsigned 16-bit integers using a Wire
    @Test
    @DisplayName("Uint16 values round-trip in raw wire")
    void uint16() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        WireSmallIntTestSupport.writeUint16Triplet(wire);
        WireSmallIntTestSupport.expectBinaryDebug(wire.bytes(), "[pos: 0, rlim: 6, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠⒉٠⒊٠‡٠٠٠٠٠٠٠٠٠٠");

        WireSmallIntTestSupport.readUint16Triplet(wire);

        assertEquals(0, bytes.readRemaining(), "uint16: no remaining bytes after read");
        wire.read();
    }

    // Test case for writing and reading unsigned 32-bit integers using a Wire
    @Test
    @DisplayName("Uint32 values round-trip in raw wire")
    void uint32() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write unsigned 32-bit integers to the wire
        wire.write().uint32(1);
        wire.write(BWKey.field1).uint32(2);

        wire.write(() -> "Test").uint32(3);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 12, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠٠٠⒉٠٠٠⒊٠٠٠‡٠٠٠٠٠٠٠٠٠٠٠٠",
                wire.bytes().toDebugString(), "Unsigned 32-bit debug output should match expected bytes");

        // Read the unsigned 32-bit integers from the wire
        @NotNull AtomicLong i = new AtomicLong();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().uint32(i, AtomicLong::set);
            assertEquals(e, i.get(), "Unsigned 32-bit value should read back as " + e);
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining(), "uint32: no remaining bytes after read");

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading signed 32-bit integers using a Wire
    @Test
    @DisplayName("Int32 values round-trip in raw wire")
    void int32() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write signed 32-bit integers to the wire
        wire.write().int32(1);
        wire.write(BWKey.field1).int32(2);

        wire.write(() -> "Test").int32(3);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 12, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠٠٠⒉٠٠٠⒊٠٠٠‡٠٠٠٠٠٠٠٠٠٠٠٠",
                wire.bytes().toDebugString(), "Signed 32-bit debug output should match expected bytes");

        // Read the signed 32-bit integers from the wire
        @NotNull AtomicInteger i = new AtomicInteger();
        IntStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int32(i, AtomicInteger::set);
            assertEquals(e, i.get(), "Signed 32-bit value should read back as " + e);
        });

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining(), "int32: no remaining bytes after read");

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading signed 64-bit integers using a Wire
    @Test
    @DisplayName("Int64 values round-trip in raw wire")
    void int64() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write signed 64-bit integers to the wire
        WireNumericTestSupport.writeInt64s(wire);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 24, wlim: 8EiB, cap: 8EiB ] ǁ⒈٠٠٠٠٠٠٠⒉٠٠٠٠٠٠٠⒊٠٠٠٠٠٠٠‡٠٠٠٠٠٠٠٠",
                wire.bytes().toDebugString(), "Signed 64-bit debug output should match expected bytes");

        // ok as blank matches anything
        @NotNull AtomicLong i = new AtomicLong();
        IntConsumer ic = i::set;
        assertNotNull(ic, "Value consumer should be set for 64-bit reads");
        WireNumericTestSupport.assertInt64sRead(wire, false);
    }

    // Test case for writing and reading 64-bit floating-point numbers using a Wire
    @Test
    @DisplayName("Float64 values round-trip in raw wire")
    void float64() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write 64-bit floating-point numbers to the wire
        WireNumericTestSupport.writeFloat64s(wire);

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 24, wlim: 8EiB, cap: 8EiB ] ǁ٠٠٠٠٠٠ð?٠٠٠٠٠٠٠@٠٠٠٠٠٠⒏@‡٠٠٠٠٠٠٠٠",
                wire.bytes().toDebugString(), "64-bit float debug output should match expected bytes");

        // ok as blank matches anything
        WireNumericTestSupport.assertFloat64sRead(wire);
    }

    // Test case for writing and reading textual data using a Wire
    @Test
    @DisplayName("Text values round-trip in raw wire")
    void text() {
        // Create a new Wire instance
        @NotNull Wire wire = createWire();

        // Write textual data to the wire
        @NotNull String name1 = "Long field name which is more than 32 characters, \\ \nBye";
        WireStringTestSupport.writeStrings(wire, name1);
        @NotNull String actual = wire.bytes().toDebugString();

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 69, wlim: 8EiB, cap: 8EiB ] ǁ⒌Hello⒌world8Long field name which is more than 32 characters, \\ ⒑Bye‡٠٠٠٠٠٠٠٠",
                actual, "Text debug output should match expected bytes");

        // Read the textual data from the wire
        WireStringTestSupport.assertReadStrings(wire, name1);

        // Verify no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining(), "text: no remaining bytes after read");

        // Ensure no issues when attempting to read beyond available data
        wire.read();
    }

    // Test case for writing and reading type prefixes using a Wire
    @Test
    @DisplayName("Type prefixes round-trip in raw wire")
    void type() {
        @NotNull Wire wire = createWire();

        // Writing type prefixes to the wire
        wire.write().typePrefix("MyType");
        wire.write(BWKey.field1).typePrefix("AlsoMyType");
        @NotNull String name1 = "com.sun.java.swing.plaf.nimbus.InternalFrameInternalFrameTitlePaneInternalFrameTitlePaneMaximizeButtonWindowNotFocusedState";

        wire.write(() -> "Test").typePrefix(name1);

        // Write an empty comment to the wire (may be a special operation in your context)
        wire.writeComment("");

        // Verify the debug representation of the written data
        assertEquals("[pos: 0, rlim: 142, wlim: 8EiB, cap: 8EiB ] ǁ⒍MyType⒑AlsoMyType{" + name1 + "‡٠٠٠٠٠٠٠٠",
                wire.bytes().toDebugString(), "Type prefix debug output should match expected bytes");

        // Read type prefixes from the wire and validate them
        Stream.of("MyType", "AlsoMyType", name1).forEach(e ->
                wire.read().typePrefix(e, StringUtils::isEqual));

        // Ensure no remaining bytes in the wire
        assertEquals(0, bytes.readRemaining(), "typePrefix: no remaining bytes after read");

        // Confirm it's safe to read beyond available data
        wire.read();
    }

    // Test case for writing and reading boolean values using a Wire
    @Test
    @DisplayName("Boolean values round-trip in raw wire")
    void testBool() {
        @NotNull Wire wire = createWire();

        WirePrimitiveTestSupport.assertBooleanRoundTrip(wire);
    }

    // Test case for writing and reading 32-bit floating-point numbers using a Wire
    @Test
    @DisplayName("Float32 values round-trip in raw wire")
    void testFloat32() {
        @NotNull Wire wire = createWire();

        WirePrimitiveTestSupport.assertFloat32RoundTrip(wire, this);
    }

    // Test case for writing and reading LocalTime objects using a Wire
    @Test
    @DisplayName("LocalTime values round-trip in raw wire")
    void testTime() {
        @NotNull Wire wire = createWire();
        LocalTime now = LocalTime.now();

        WirePrimitiveTestSupport.writeTimes(wire, now);
        WirePrimitiveTestSupport.assertTimes(wire, now);
    }

    // Test case for writing and reading ZonedDateTime objects using a Wire
    @Test
    @DisplayName("ZonedDateTime values round-trip in raw wire")
    void testZonedDateTime() {
        @NotNull Wire wire = createWire();
        WireTemporalTestSupport.assertZonedDateTimes(wire);
    }

    // Test case for writing and reading LocalDate objects using a Wire
    @Test
    @DisplayName("LocalDate values round-trip in raw wire")
    void testDate() {
        @NotNull Wire wire = createWire();
        WireTemporalTestSupport.assertLocalDates(wire);
    }

    // Test case for writing and reading UUID objects using a Wire
    @Test
    @DisplayName("UUID values round-trip in raw wire")
    void testUuid() {
        @NotNull Wire wire = createWire();
        WireTemporalTestSupport.assertUuids(wire);
    }

    // Test case for writing and reading byte arrays using a Wire
    // Currently, this test is ignored due to an UnsupportedOperationException
    @Test
    @Disabled("todo fix :currently using NoBytesStore so will fail with UnsupportedOperationException")
    @SuppressWarnings("rawtypes")
    @DisplayName("Byte arrays round-trip in raw wire")
    void testBytes() {
        @NotNull Wire wire = createWire();
        @NotNull byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++)
            allBytes[i] = (byte) i;

        WireBytesTestSupport.exerciseBytesRoundTrip(wire, WireBytesTestSupport.helloBytes(), WireBytesTestSupport.quoteBytes(), allBytes);

        @NotNull NativeBytes allBytes2 = nativeBytes();
        WireBytesTestSupport.assertBytesRoundTrip(wire, allBytes, allBytes2);
    }

    // Test case for writing and reading custom Marshallable objects using a Wire
    @Test
    @DisplayName("Marshallable objects round-trip with event names")
    void testWriteMarshallable() {
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = (true);
        mtA.d = (123.456);
        mtA.i = (-12345789);
        mtA.s = ((short) 12345);
        mtA.text.append("Hello World");

        @NotNull Wire wire = createWire();
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
                wire.bytes().toDebugString(), "Marshallable debug output should match expected bytes");

        @NotNull MyTypesCustom mt2 = new MyTypesCustom();
        @NotNull StringBuilder key = new StringBuilder();
        // Reading and validating MyTypesCustom objects from the wire
        wire.readEventName(key).marshallable(mt2);
        assertEquals("A", key.toString(), "First event name should be A");
        assertEquals(mt2, mtA, "First marshallable should round trip");

        wire.readEventName(key).marshallable(mt2);
        assertEquals("B", key.toString(), "Second event name should be B");
        assertEquals(mt2, mtB, "Second marshallable should round trip");
    }

    // Enum representing keys for Wire operations
    enum BWKey implements WireKey {
        field1, field2, field3
    }
}
