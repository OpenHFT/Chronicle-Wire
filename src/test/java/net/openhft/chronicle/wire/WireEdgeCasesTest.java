/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-cutting edge case tests that run against multiple wire types.
 * Tests empty/null handling, boundary values, and round-trip consistency.
 */
@SuppressWarnings({"deprecation", "removal"})
public class WireEdgeCasesTest extends WireTestCommon {

    private static final WireType[] ALL_WIRE_TYPES = {
        WireType.BINARY, WireType.TEXT, WireType.YAML
    };

    // ========== Empty/Null Handling Across Wire Types ==========

    @Test
    @DisplayName("Tests empty string handling across wire types")
    public void testEmptyStringAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("empty").text("");
            bytes.readPosition(0);

            assertEquals("", wire.read("empty").text(),
                "Empty string should round-trip in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests null string handling across wire types")
    public void testNullStringAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("null").text((String) null);
            bytes.readPosition(0);

            assertNull(wire.read("null").text(),
                "Null string should round-trip in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests empty byte array handling across wire types")
    public void testEmptyByteArrayAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            byte[] empty = new byte[0];
            wire.write("empty").bytes(empty);
            bytes.readPosition(0);

            byte[] result = wire.read("empty").bytes();
            assertNotNull(result, "Empty byte array should not be null in " + wireType);
            assertEquals(0, result.length, "Empty byte array length should be 0 in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests empty sequence handling across wire types")
    public void testEmptySequenceAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("empty").sequence(v -> { });
            bytes.readPosition(0);

            List<Integer> items = new ArrayList<>();
            wire.read("empty").sequence(items, (list, v) -> {
                while (v.hasNextSequenceItem()) {
                    list.add(v.int32());
                }
            });
            assertTrue(items.isEmpty(), "Empty sequence should have no items in " + wireType);
        }
    }

    // TODO FIX: Lambda access issue when using marshallable consumer across wire types
    @Test
    @Disabled("IllegalAccess with lambda in marshallable - Java module access issue")
    @DisplayName("Tests empty marshallable handling across wire types")
    public void testEmptyMarshallableAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("empty").marshallable(w -> { });
            bytes.readPosition(0);

            AtomicInteger count = new AtomicInteger(0);
            wire.read("empty").marshallable(w -> {
                while (w.hasMore()) {
                    w.read().skipValue();
                    count.incrementAndGet();
                }
            });
            assertEquals(0, count.get(), "Empty marshallable should have no fields in " + wireType);
        }
    }

    // ========== Numeric Boundary Values Across Wire Types ==========

    @Test
    @DisplayName("Tests byte boundary values across wire types")
    public void testByteBoundariesAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("min").int8(Byte.MIN_VALUE);
            wire.write("max").int8(Byte.MAX_VALUE);
            wire.write("zero").int8((byte) 0);
            wire.write("negOne").int8((byte) -1);

            bytes.readPosition(0);

            assertEquals(Byte.MIN_VALUE, wire.read("min").int8(),
                "Byte.MIN_VALUE should round-trip in " + wireType);
            assertEquals(Byte.MAX_VALUE, wire.read("max").int8(),
                "Byte.MAX_VALUE should round-trip in " + wireType);
            assertEquals(0, wire.read("zero").int8(),
                "Zero byte should round-trip in " + wireType);
            assertEquals(-1, wire.read("negOne").int8(),
                "-1 byte should round-trip in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests short boundary values across wire types")
    public void testShortBoundariesAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("min").int16(Short.MIN_VALUE);
            wire.write("max").int16(Short.MAX_VALUE);
            wire.write("zero").int16((short) 0);

            bytes.readPosition(0);

            assertEquals(Short.MIN_VALUE, wire.read("min").int16(),
                "Short.MIN_VALUE should round-trip in " + wireType);
            assertEquals(Short.MAX_VALUE, wire.read("max").int16(),
                "Short.MAX_VALUE should round-trip in " + wireType);
            assertEquals(0, wire.read("zero").int16(),
                "Zero short should round-trip in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests int boundary values across wire types")
    public void testIntBoundariesAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("min").int32(Integer.MIN_VALUE);
            wire.write("max").int32(Integer.MAX_VALUE);
            wire.write("zero").int32(0);

            bytes.readPosition(0);

            assertEquals(Integer.MIN_VALUE, wire.read("min").int32(),
                "Integer.MIN_VALUE should round-trip in " + wireType);
            assertEquals(Integer.MAX_VALUE, wire.read("max").int32(),
                "Integer.MAX_VALUE should round-trip in " + wireType);
            assertEquals(0, wire.read("zero").int32(),
                "Zero int should round-trip in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests long boundary values across wire types")
    public void testLongBoundariesAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            // Note: Long.MIN_VALUE may have special handling
            wire.write("minPlusOne").int64(Long.MIN_VALUE + 1);
            wire.write("max").int64(Long.MAX_VALUE);
            wire.write("zero").int64(0L);

            bytes.readPosition(0);

            assertEquals(Long.MIN_VALUE + 1, wire.read("minPlusOne").int64(),
                "Long.MIN_VALUE+1 should round-trip in " + wireType);
            assertEquals(Long.MAX_VALUE, wire.read("max").int64(),
                "Long.MAX_VALUE should round-trip in " + wireType);
            assertEquals(0L, wire.read("zero").int64(),
                "Zero long should round-trip in " + wireType);
        }
    }

    // ========== Float/Double Special Values Across Wire Types ==========

    @Test
    @DisplayName("Tests special float values across wire types")
    public void testSpecialFloatsAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(512);
            Wire wire = wireType.apply(bytes);

            wire.write("nan").float32(Float.NaN);
            wire.write("posInf").float32(Float.POSITIVE_INFINITY);
            wire.write("negInf").float32(Float.NEGATIVE_INFINITY);
            wire.write("minVal").float32(Float.MIN_VALUE);
            wire.write("maxVal").float32(Float.MAX_VALUE);

            bytes.readPosition(0);

            assertTrue(Float.isNaN(wire.read("nan").float32()),
                "Float.NaN should round-trip in " + wireType);
            assertEquals(Float.POSITIVE_INFINITY, wire.read("posInf").float32(),
                "Float.POSITIVE_INFINITY should round-trip in " + wireType);
            assertEquals(Float.NEGATIVE_INFINITY, wire.read("negInf").float32(),
                "Float.NEGATIVE_INFINITY should round-trip in " + wireType);
            assertEquals(Float.MIN_VALUE, wire.read("minVal").float32(),
                "Float.MIN_VALUE should round-trip in " + wireType);
            assertEquals(Float.MAX_VALUE, wire.read("maxVal").float32(),
                "Float.MAX_VALUE should round-trip in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests special double values across wire types")
    public void testSpecialDoublesAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(512);
            Wire wire = wireType.apply(bytes);

            wire.write("nan").float64(Double.NaN);
            wire.write("posInf").float64(Double.POSITIVE_INFINITY);
            wire.write("negInf").float64(Double.NEGATIVE_INFINITY);
            wire.write("minVal").float64(Double.MIN_VALUE);
            wire.write("maxVal").float64(Double.MAX_VALUE);

            bytes.readPosition(0);

            assertTrue(Double.isNaN(wire.read("nan").float64()),
                "Double.NaN should round-trip in " + wireType);
            assertEquals(Double.POSITIVE_INFINITY, wire.read("posInf").float64(),
                "Double.POSITIVE_INFINITY should round-trip in " + wireType);
            assertEquals(Double.NEGATIVE_INFINITY, wire.read("negInf").float64(),
                "Double.NEGATIVE_INFINITY should round-trip in " + wireType);
            assertEquals(Double.MIN_VALUE, wire.read("minVal").float64(),
                "Double.MIN_VALUE should round-trip in " + wireType);
            assertEquals(Double.MAX_VALUE, wire.read("maxVal").float64(),
                "Double.MAX_VALUE should round-trip in " + wireType);
        }
    }

    // ========== Boolean Handling Across Wire Types ==========

    @Test
    @DisplayName("Tests boolean values across wire types")
    public void testBooleanAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("t").bool(true);
            wire.write("f").bool(false);

            bytes.readPosition(0);

            assertTrue(wire.read("t").bool(), "true should round-trip in " + wireType);
            assertFalse(wire.read("f").bool(), "false should round-trip in " + wireType);
        }
    }

    // ========== UUID Handling Across Wire Types ==========

    @Test
    @DisplayName("Tests UUID values across wire types")
    public void testUUIDAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            UUID random = UUID.randomUUID();
            UUID zeros = new UUID(0L, 0L);
            UUID ones = new UUID(-1L, -1L);

            wire.write("random").uuid(random);
            wire.write("zeros").uuid(zeros);
            wire.write("ones").uuid(ones);

            bytes.readPosition(0);

            assertEquals(random, wire.read("random").uuid(),
                "Random UUID should round-trip in " + wireType);
            assertEquals(zeros, wire.read("zeros").uuid(),
                "Zero UUID should round-trip in " + wireType);
            assertEquals(ones, wire.read("ones").uuid(),
                "All-ones UUID should round-trip in " + wireType);
        }
    }

    // ========== Consumer Callback Tests Across Wire Types ==========

    @Test
    @DisplayName("Tests int32 consumer callback across wire types")
    public void testInt32ConsumerAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("val").int32(12345);
            bytes.readPosition(0);

            AtomicInteger result = new AtomicInteger();
            wire.read("val").int32(result, AtomicInteger::set);
            assertEquals(12345, result.get(), "Int32 consumer should work in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests int64 consumer callback across wire types")
    public void testInt64ConsumerAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("val").int64(9876543210L);
            bytes.readPosition(0);

            AtomicLong result = new AtomicLong();
            wire.read("val").int64(result, AtomicLong::set);
            assertEquals(9876543210L, result.get(), "Int64 consumer should work in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests float64 consumer callback across wire types")
    public void testFloat64ConsumerAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("val").float64(3.14159);
            bytes.readPosition(0);

            AtomicReference<Double> result = new AtomicReference<>();
            wire.read("val").float64(result, AtomicReference::set);
            assertEquals(3.14159, result.get(), 0.00001, "Float64 consumer should work in " + wireType);
        }
    }

    // ========== Skip Value Tests Across Wire Types ==========

    @Test
    @DisplayName("SkipValue should work across all wire types")
    public void testSkipValueAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(512);
            Wire wire = wireType.apply(bytes);

            wire.write("skip1").int32(42);
            wire.write("skip2").text("skip me");
            wire.write("skip3").sequence(v -> {
                v.int32(1);
                v.int32(2);
            });
            wire.write("keep").text("found");

            bytes.readPosition(0);

            wire.read("skip1").skipValue();
            wire.read("skip2").skipValue();
            wire.read("skip3").skipValue();

            assertEquals("found", wire.read("keep").text(),
                "Skip value should work in " + wireType);
        }
    }

    // ========== Nested Structure Tests Across Wire Types ==========

    // TODO FIX: Lambda access issue when using nested marshallable consumers across wire types
    @Test
    @Disabled("IllegalAccess with nested lambda in marshallable - Java module access issue")
    @DisplayName("Tests nested marshallable across wire types")
    public void testNestedMarshallableAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(512);
            Wire wire = wireType.apply(bytes);

            wire.write("outer").marshallable(outer -> outer.write("inner").marshallable(inner -> inner.write("value").int32(42)));

            bytes.readPosition(0);

            AtomicInteger value = new AtomicInteger();
            wire.read("outer").marshallable(outer -> outer.read("inner").marshallable(inner -> value.set(inner.read("value").int32())));

            assertEquals(42, value.get(), "Nested marshallable should work in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests nested sequences across wire types")
    public void testNestedSequencesAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(512);
            Wire wire = wireType.apply(bytes);

            wire.write("outer").sequence(outer -> {
                outer.sequence(inner -> {
                    inner.int32(1);
                    inner.int32(2);
                });
                outer.sequence(inner -> {
                    inner.int32(3);
                    inner.int32(4);
                });
            });

            bytes.readPosition(0);

            List<List<Integer>> result = new ArrayList<>();
            wire.read("outer").sequence(result, (list, outer) -> {
                while (outer.hasNextSequenceItem()) {
                    List<Integer> inner = new ArrayList<>();
                    outer.sequence(inner, (innerList, v) -> {
                        while (v.hasNextSequenceItem()) {
                            innerList.add(v.int32());
                        }
                    });
                    list.add(inner);
                }
            });

            assertEquals(2, result.size(), "Nested sequences should have 2 inner in " + wireType);
            assertEquals(2, result.get(0).size(), "First inner should have 2 items in " + wireType);
            assertEquals(1, result.get(0).get(0), "First item should be 1 in " + wireType);
        }
    }

    // ========== Mixed Type Sequences Across Wire Types ==========

    @Test
    @DisplayName("Tests sequence with mixed types across wire types")
    public void testMixedTypeSequenceAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(512);
            Wire wire = wireType.apply(bytes);

            wire.write("mixed").sequence(v -> {
                v.int32(42);
                v.text("hello");
                v.float64(3.14);
                v.bool(true);
            });

            bytes.readPosition(0);

            Object[] values = new Object[4];
            wire.read("mixed").sequence(values, (arr, v) -> {
                arr[0] = v.int32();
                arr[1] = v.text();
                arr[2] = v.float64();
                arr[3] = v.bool();
            });

            assertEquals(42, values[0], "Int should read in mixed sequence for " + wireType);
            assertEquals("hello", values[1], "String should read in mixed sequence for " + wireType);
            assertEquals(3.14, (Double) values[2], 0.001, "Double should read in mixed sequence for " + wireType);
            assertEquals(true, values[3], "Boolean should read in mixed sequence for " + wireType);
        }
    }

    // ========== Bracket Type Tests Across Wire Types ==========

    @Test
    @DisplayName("Tests getBracketType for sequence across wire types")
    public void testBracketTypeSequenceAcrossWireTypes() {
        // Skip BinaryWire as it uses different encoding
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML}) {
            Wire wire;
            if (wireType == WireType.TEXT) {
                wire = TextWire.from("field: [1, 2, 3]");
            } else {
                wire = YamlWire.from("field: [1, 2, 3]");
            }
            wire.read("field");
            BracketType type = wire.getValueIn().getBracketType();
            assertEquals(BracketType.SEQ, type, "Bracket type should be SEQ in " + wireType);
        }
    }

    @Test
    @DisplayName("Tests getBracketType for mapping across wire types")
    public void testBracketTypeMappingAcrossWireTypes() {
        // Skip BinaryWire as it uses different encoding
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML}) {
            Wire wire;
            if (wireType == WireType.TEXT) {
                wire = TextWire.from("field: { a: 1 }");
            } else {
                wire = YamlWire.from("field: { a: 1 }");
            }
            wire.read("field");
            BracketType type = wire.getValueIn().getBracketType();
            assertEquals(BracketType.MAP, type, "Bracket type should be MAP in " + wireType);
        }
    }

    // ========== Character Tests Across Wire Types ==========

    @Test
    @DisplayName("Tests character values across wire types")
    public void testCharacterAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("a").text("A");
            wire.write("space").text(" ");

            bytes.readPosition(0);

            assertEquals('A', wire.read("a").character(),
                "Character A should round-trip in " + wireType);
            assertEquals(' ', wire.read("space").character(),
                "Space character should round-trip in " + wireType);
        }
    }

    // ========== Text StringBuilder Tests ==========

    @Test
    @DisplayName("Tests textTo StringBuilder across wire types")
    public void testTextToStringBuilderAcrossWireTypes() {
        for (WireType wireType : ALL_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            Wire wire = wireType.apply(bytes);

            wire.write("text").text("hello world");
            bytes.readPosition(0);

            StringBuilder sb = new StringBuilder();
            wire.read("text").textTo(sb);
            assertEquals("hello world", sb.toString(),
                "Text to StringBuilder should work in " + wireType);
        }
    }
}
