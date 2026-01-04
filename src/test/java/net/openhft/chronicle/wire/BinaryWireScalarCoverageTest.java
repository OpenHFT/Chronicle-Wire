/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BinaryWireScalarCoverageTest extends WireTestCommon {

    @Test
    @DisplayName("Round-trips common scalar values in binary wire")
    void roundTripsCommonScalarTypes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("i32").int32(123);
        wire.write("i64").int64(Long.MIN_VALUE + 7);
        wire.write("boolTrue").bool(true);
        wire.write("boolFalse").bool(false);
        wire.write("text").text("hello");
        wire.write("float").float64(3.14159);
        wire.write("seq").sequence(v -> {
            v.int16((short) 1);
            v.text("two");
            v.int64(3L);
        });

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(123, wire.read("i32").int32(), "int32 should round-trip");
        assertEquals(Long.MIN_VALUE + 7, wire.read("i64").int64(), "int64 should round-trip");
        assertTrue(wire.read("boolTrue").bool(), "boolTrue should round-trip");
        assertFalse(wire.read("boolFalse").bool(), "boolFalse should round-trip");
        assertEquals("hello", wire.read("text").text(), "text should round-trip");
        assertEquals(3.14159, wire.read("float").float64(), 0.0, "float should round-trip");

        Object[] holder = new Object[3];
        wire.read("seq").sequence(holder, (arr, in) -> {
            arr[0] = in.int16();
            arr[1] = in.text();
            arr[2] = in.int64();
        });
        assertEquals((short) 1, holder[0], "Sequence item 0 should be int16 value 1");
        assertEquals("two", holder[1], "Sequence item 1 should be text value two");
        assertEquals(3L, holder[2], "Sequence item 2 should be int64 value 3");
    }

    @Test
    @DisplayName("BinaryWire should round-trip int8 scalar values")
    @SuppressWarnings("deprecation")
    void testInt8Values() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("min").int8(Byte.MIN_VALUE);
        wire.write("max").int8(Byte.MAX_VALUE);
        wire.write("zero").int8((byte) 0);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(Byte.MIN_VALUE, wire.read("min").int8(), "BinaryWire should read min byte value");
        assertEquals(Byte.MAX_VALUE, wire.read("max").int8(), "BinaryWire should read max byte value");
        assertEquals(0, wire.read("zero").int8(), "BinaryWire should read zero byte value");
    }

    @Test
    @DisplayName("BinaryWire should round-trip uint8 scalar values")
    @SuppressWarnings("deprecation")
    void testUint8Values() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("max").uint8(255);
        wire.write("mid").uint8(128);
        wire.write("zero").uint8(0);

        bytes.readPositionRemaining(0, bytes.writePosition());

        AtomicInteger maxResult = new AtomicInteger();
        AtomicInteger midResult = new AtomicInteger();
        AtomicInteger zeroResult = new AtomicInteger();
        wire.read("max").uint8(maxResult, AtomicInteger::set);
        wire.read("mid").uint8(midResult, AtomicInteger::set);
        wire.read("zero").uint8(zeroResult, AtomicInteger::set);
        assertEquals(255, maxResult.get(), "BinaryWire should read max unsigned byte value");
        assertEquals(128, midResult.get(), "BinaryWire should read mid unsigned byte value");
        assertEquals(0, zeroResult.get(), "BinaryWire should read zero unsigned byte value");
    }

    @Test
    @DisplayName("BinaryWire should round-trip int16 scalar values")
    void testInt16Values() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("min").int16(Short.MIN_VALUE);
        wire.write("max").int16(Short.MAX_VALUE);
        wire.write("zero").int16((short) 0);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(Short.MIN_VALUE, wire.read("min").int16(), "BinaryWire should read min short value");
        assertEquals(Short.MAX_VALUE, wire.read("max").int16(), "BinaryWire should read max short value");
        assertEquals(0, wire.read("zero").int16(), "BinaryWire should read zero short value");
    }

    @Test
    @DisplayName("BinaryWire should round-trip uint16 scalar values")
    void testUint16Values() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("max").uint16(65535);
        wire.write("mid").uint16(32768);
        wire.write("zero").uint16(0);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(65535, wire.read("max").uint16(), "BinaryWire should read max unsigned short value");
        assertEquals(32768, wire.read("mid").uint16(), "BinaryWire should read mid unsigned short value");
        assertEquals(0, wire.read("zero").uint16(), "BinaryWire should read zero unsigned short value");
    }

    @Test
    @DisplayName("BinaryWire should round-trip uint32 scalar values")
    @SuppressWarnings("deprecation")
    void testUint32Values() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("max").uint32(4294967295L);
        wire.write("mid").uint32(2147483648L);
        wire.write("zero").uint32(0L);

        bytes.readPositionRemaining(0, bytes.writePosition());

        AtomicLong maxResult = new AtomicLong();
        AtomicLong midResult = new AtomicLong();
        AtomicLong zeroResult = new AtomicLong();
        wire.read("max").uint32(maxResult, AtomicLong::set);
        wire.read("mid").uint32(midResult, AtomicLong::set);
        wire.read("zero").uint32(zeroResult, AtomicLong::set);
        assertEquals(4294967295L, maxResult.get(), "BinaryWire should read max unsigned int value");
        assertEquals(2147483648L, midResult.get(), "BinaryWire should read mid unsigned int value");
        assertEquals(0L, zeroResult.get(), "BinaryWire should read zero unsigned int value");
    }

    @Test
    @DisplayName("BinaryWire should round-trip float32 scalar values")
    void testFloat32Values() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("pi").float32(3.14159f);
        wire.write("neg").float32(-1.5f);
        wire.write("zero").float32(0.0f);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(3.14159f, wire.read("pi").float32(), 0.0001f, "BinaryWire should read pi float32 value");
        assertEquals(-1.5f, wire.read("neg").float32(), 0.0001f, "BinaryWire should read negative float32 value");
        assertEquals(0.0f, wire.read("zero").float32(), 0.0001f, "BinaryWire should read zero float32 value");
    }

    @Test
    @DisplayName("BinaryWire should read NaN and infinity values")
    void testSpecialFloatValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("nan").float64(Double.NaN);
        wire.write("inf").float64(Double.POSITIVE_INFINITY);
        wire.write("ninf").float64(Double.NEGATIVE_INFINITY);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertTrue(Double.isNaN(wire.read("nan").float64()), "BinaryWire should read NaN float64 value");
        assertEquals(Double.POSITIVE_INFINITY, wire.read("inf").float64(), "BinaryWire should read positive infinity float64 value");
        assertEquals(Double.NEGATIVE_INFINITY, wire.read("ninf").float64(), "BinaryWire should read negative infinity float64 value");
    }

    @Test
    @DisplayName("BinaryWire should round-trip LocalDate value instances")
    void testLocalDate() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        LocalDate date = LocalDate.of(2024, 6, 15);
        wire.write("date").object(date);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(date, wire.read("date").object(LocalDate.class), "BinaryWire should read LocalDate value instance");
    }

    @Test
    @DisplayName("BinaryWire should round-trip LocalTime value instances")
    void testLocalTime() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        LocalTime time = LocalTime.of(14, 30, 45);
        wire.write("time").object(time);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(time, wire.read("time").object(LocalTime.class), "BinaryWire should read LocalTime value instance");
    }

    @Test
    @DisplayName("BinaryWire should round-trip LocalDateTime value instances")
    void testLocalDateTime() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        LocalDateTime dt = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
        wire.write("dt").object(dt);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(dt, wire.read("dt").object(LocalDateTime.class), "BinaryWire should read LocalDateTime value instance");
    }

    @Test
    @DisplayName("BinaryWire should round-trip ZonedDateTime value instances")
    void testZonedDateTime() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        ZonedDateTime zdt = ZonedDateTime.of(2024, 6, 15, 14, 30, 45, 0, ZoneId.of("UTC"));
        wire.write("zdt").object(zdt);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(zdt, wire.read("zdt").object(ZonedDateTime.class), "BinaryWire should read ZonedDateTime value instance");
    }

    @Test
    @DisplayName("BinaryWire should round-trip UUID value instances")
    void testUUID() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        UUID uuid = UUID.randomUUID();
        wire.write("uuid").uuid(uuid);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(uuid, wire.read("uuid").uuid(), "BinaryWire should read UUID value instance");
    }

    @Test
    @DisplayName("BinaryWire should round-trip Map entry values")
    void testMap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        BinaryWire wire = new BinaryWire(bytes);
        wire.write("map").object(map);

        bytes.readPositionRemaining(0, bytes.writePosition());

        @SuppressWarnings("unchecked")
        Map<String, Integer> result = wire.read("map").object(Map.class);
        assertEquals(3, result.size(), "Map should have 3 entries");
        assertEquals(1, result.get("one"), "Map entry 'one' should equal 1 after round-trip");
        assertEquals(2, result.get("two"), "Map entry 'two' should equal 2 after round-trip");
        assertEquals(3, result.get("three"), "Map entry 'three' should equal 3 after round-trip");
    }

    @Test
    @DisplayName("BinaryWire should round-trip List entry values")
    void testList() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        List<String> list = new ArrayList<>();
        list.add("alpha");
        list.add("beta");
        list.add("gamma");
        BinaryWire wire = new BinaryWire(bytes);
        wire.write("list").object(list);

        bytes.readPositionRemaining(0, bytes.writePosition());

        @SuppressWarnings("unchecked")
        List<String> result = wire.read("list").object(List.class);
        assertEquals(3, result.size(), "List should have 3 items");
        assertEquals("alpha", result.get(0), "List index 0 should equal alpha element");
        assertEquals("beta", result.get(1), "List index 1 should equal beta element");
        assertEquals("gamma", result.get(2), "List index 2 should equal gamma element");
    }

    @Test
    @DisplayName("BinaryWire should round-trip byte array values")
    void testBytesArray() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        wire.write("data").bytes(data);

        bytes.readPositionRemaining(0, bytes.writePosition());

        byte[] result = wire.read("data").bytes();
        assertArrayEquals(data, result, "Bytes array should round-trip");
    }

    @Test
    @DisplayName("BinaryWire should round-trip null object values")
    void testNullValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("null").object(null);

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertNull(wire.read("null").object(), "BinaryWire should read null object value");
    }

    @Test
    @DisplayName("BinaryWire should round-trip empty string values")
    void testEmptyString() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("empty").text("");

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals("", wire.read("empty").text(), "BinaryWire should read empty string value");
    }

    @Test
    @DisplayName("BinaryWire should skip first value and read next entry")
    void testSkipValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("skip").text("skipped");
        wire.write("keep").text("kept");

        bytes.readPositionRemaining(0, bytes.writePosition());

        wire.read("skip").skipValue();
        assertEquals("kept", wire.read("keep").text(),
                "BinaryWire should skip first value before reading keep entry");
    }

    @Test
    @DisplayName("BinaryWire should deliver int32 value to consumer")
    @SuppressWarnings("deprecation")
    void testInt32WithConsumer() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("val").int32(42);

        bytes.readPositionRemaining(0, bytes.writePosition());

        AtomicInteger result = new AtomicInteger();
        wire.read("val").int32(result, AtomicInteger::set);
        assertEquals(42, result.get(), "int32 consumer should receive 42 from key 'val'");
    }

    @Test
    @DisplayName("BinaryWire should deliver int64 value to consumer")
    void testInt64WithConsumer() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("val").int64(Long.MAX_VALUE);

        bytes.readPositionRemaining(0, bytes.writePosition());

        AtomicLong result = new AtomicLong();
        wire.read("val").int64(result, AtomicLong::set);
        assertEquals(Long.MAX_VALUE, result.get(), "int64 consumer should receive Long.MAX_VALUE from key 'val'");
    }

    @Test
    @DisplayName("BinaryWire should deliver float64 value to consumer")
    void testFloat64WithConsumer() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("val").float64(3.14159);

        bytes.readPositionRemaining(0, bytes.writePosition());

        AtomicReference<Double> result = new AtomicReference<>();
        wire.read("val").float64(result, AtomicReference::set);
        assertEquals(3.14159, result.get(), 0.0001, "float64 consumer should receive 3.14159 from key 'val'");
    }

    @Test
    @DisplayName("BinaryWire should deliver boolean values to consumer")
    @SuppressWarnings("deprecation")
    void testBoolWithConsumer() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("t").bool(true);
        wire.write("f").bool(false);

        bytes.readPositionRemaining(0, bytes.writePosition());

        AtomicBoolean resultT = new AtomicBoolean(false);
        AtomicBoolean resultF = new AtomicBoolean(true);
        wire.read("t").bool(resultT, (ref, val) -> ref.set(val != null && val));
        wire.read("f").bool(resultF, (ref, val) -> ref.set(val != null && val));
        assertTrue(resultT.get(), "BinaryWire should deliver true value to consumer");
        assertFalse(resultF.get(), "BinaryWire should deliver false value to consumer");
    }

    @Test
    @DisplayName("BinaryWire should report binary wire format")
    void testIsBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);
        assertTrue(wire.isBinary(), "BinaryWire should report binary wire format as true");
    }

    @Test
    @DisplayName("BinaryWire should read nested marshallable values")
    void testNestedMarshallable() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("outer").marshallable(w -> w.write("inner").marshallable(m -> m.write("value").int32(42)));

        bytes.readPositionRemaining(0, bytes.writePosition());

        AtomicInteger value = new AtomicInteger();
        wire.read("outer").marshallable(w -> w.read("inner").marshallable(m -> value.set(m.read("value").int32())));
        assertEquals(42, value.get(), "BinaryWire should read nested marshallable value 42");
    }

    @Test
    @DisplayName("BinaryWire should read sequence using hasNextSequenceItem")
    void testSequenceHasNextItem() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        List<Integer> source = new ArrayList<>();
        source.add(1);
        source.add(2);
        source.add(3);
        BinaryWire wire = new BinaryWire(bytes);
        wire.write("seq").sequence(source, (s, v) -> {
            for (int val : s) {
                v.int32(val);
            }
        });

        bytes.readPositionRemaining(0, bytes.writePosition());

        List<Integer> items = new ArrayList<>();
        wire.read("seq").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.int32());
            }
        });
        assertEquals(3, items.size(), "BinaryWire should read three sequence items");
        assertEquals(1, items.get(0), "Sequence index 0 should equal expected value 1");
        assertEquals(2, items.get(1), "Sequence index 1 should equal expected value 2");
        assertEquals(3, items.get(2), "Sequence index 2 should equal expected value 3");
    }
}
