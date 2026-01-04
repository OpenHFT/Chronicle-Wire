/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for integer overflow, sign handling, and numeric parsing edge cases.
 * Based on java.lang.Long parsing semantics and overflow behaviour.
 */
@SuppressWarnings({"deprecation", "removal"})
public class WireIntegerOverflowTest extends WireTestCommon {

    // ========== Long Boundary Tests ==========

    @Test
    @DisplayName("All wire types should handle Long.MAX_VALUE exactly")
    public void testLongMaxValue() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int64(Long.MAX_VALUE);
            bytes.readPosition(0);

            assertEquals(Long.MAX_VALUE, wire.read("val").int64(),
                    "Long.MAX_VALUE should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle Long.MIN_VALUE exactly")
    public void testLongMinValue() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int64(Long.MIN_VALUE);
            bytes.readPosition(0);

            assertEquals(Long.MIN_VALUE, wire.read("val").int64(),
                    "Long.MIN_VALUE should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle Long.MAX_VALUE - 1")
    public void testLongMaxValueMinus1() {
        long value = Long.MAX_VALUE - 1;

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int64(value);
            bytes.readPosition(0);

            assertEquals(value, wire.read("val").int64(),
                    "Long.MAX_VALUE - 1 should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle Long.MIN_VALUE + 1")
    public void testLongMinValuePlus1() {
        long value = Long.MIN_VALUE + 1;

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int64(value);
            bytes.readPosition(0);

            assertEquals(value, wire.read("val").int64(),
                    "Long.MIN_VALUE + 1 should round-trip in " + wt);
        }
    }

    // ========== Integer Boundary Tests ==========

    @Test
    @DisplayName("All wire types should handle Integer.MAX_VALUE exactly")
    public void testIntMaxValue() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int32(Integer.MAX_VALUE);
            bytes.readPosition(0);

            assertEquals(Integer.MAX_VALUE, wire.read("val").int32(),
                    "Integer.MAX_VALUE should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle Integer.MIN_VALUE exactly")
    public void testIntMinValue() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int32(Integer.MIN_VALUE);
            bytes.readPosition(0);

            assertEquals(Integer.MIN_VALUE, wire.read("val").int32(),
                    "Integer.MIN_VALUE should round-trip in " + wt);
        }
    }

    // ========== Byte and Short Boundary Tests ==========

    @Test
    @DisplayName("All wire types should handle Byte boundaries")
    public void testByteBoundaries() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("max").int8(Byte.MAX_VALUE);
            wire.write("min").int8(Byte.MIN_VALUE);
            wire.write("zero").int8((byte) 0);
            wire.write("negOne").int8((byte) -1);

            bytes.readPosition(0);

            assertEquals(Byte.MAX_VALUE, wire.read("max").int8(), "Byte.MAX_VALUE in " + wt);
            assertEquals(Byte.MIN_VALUE, wire.read("min").int8(), "Byte.MIN_VALUE in " + wt);
            assertEquals(0, wire.read("zero").int8(), "Byte zero should round-trip in " + wt);
            assertEquals(-1, wire.read("negOne").int8(), "Byte minus one should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle Short boundaries")
    public void testShortBoundaries() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("max").int16(Short.MAX_VALUE);
            wire.write("min").int16(Short.MIN_VALUE);
            wire.write("aboveByteMax").int16((short) (Byte.MAX_VALUE + 1));
            wire.write("belowByteMin").int16((short) (Byte.MIN_VALUE - 1));

            bytes.readPosition(0);

            assertEquals(Short.MAX_VALUE, wire.read("max").int16(), "Short.MAX_VALUE in " + wt);
            assertEquals(Short.MIN_VALUE, wire.read("min").int16(), "Short.MIN_VALUE in " + wt);
            assertEquals(Byte.MAX_VALUE + 1, wire.read("aboveByteMax").int16(),
                    "Above Byte.MAX_VALUE in " + wt);
            assertEquals(Byte.MIN_VALUE - 1, wire.read("belowByteMin").int16(),
                    "Below Byte.MIN_VALUE in " + wt);
        }
    }

    // ========== JS Safe Integer Boundary Tests ==========

    @Test
    @DisplayName("All wire types should handle JS safe integer boundary (2^53)")
    public void testJsSafeIntegerBoundary() {
        // JavaScript safe integer max: 2^53 = 9007199254740992
        long jsSafeMax = 9007199254740992L;
        // First unsafe: 2^53 + 1 = 9007199254740993
        long jsUnsafe = 9007199254740993L;

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("safe").int64(jsSafeMax);
            wire.write("unsafe").int64(jsUnsafe);

            bytes.readPosition(0);

            assertEquals(jsSafeMax, wire.read("safe").int64(),
                    "JS safe integer max should round-trip in " + wt);
            assertEquals(jsUnsafe, wire.read("unsafe").int64(),
                    "First JS unsafe integer should round-trip in " + wt);
        }
    }

    // ========== Text Wire Numeric Parsing Tests ==========

    @Test
    @DisplayName("TextWire should parse numbers with leading plus sign")
    public void testLeadingPlusSign() {
        String yaml = "val: +42\n";
        Bytes<?> bytes = Bytes.from(yaml);
        TextWire wire = new TextWire(bytes);

        int value = wire.read("val").int32();
        assertEquals(42, value, "Leading + should be accepted");
    }

    @Test
    @DisplayName("YamlWire should parse numbers with leading plus sign")
    public void testLeadingPlusSignYaml() {
        String yaml = "val: +42\n";
        Bytes<?> bytes = Bytes.from(yaml);
        YamlWire wire = new YamlWire(bytes);

        int value = wire.read("val").int32();
        assertEquals(42, value, "Leading + should be accepted in YAML");
    }

    @Test
    @DisplayName("TextWire should parse hexadecimal numbers from text")
    public void testHexNumbers() {
        String yaml = "val: 0xFF\n";
        Bytes<?> bytes = Bytes.from(yaml);
        TextWire wire = new TextWire(bytes);

        int value = wire.read("val").int32();
        assertEquals(255, value, "0xFF should parse as 255");
    }

    @Test
    @DisplayName("TextWire should parse zero hexadecimal values")
    public void testZeroHex() {
        String yaml = "val: 0x0\n";
        Bytes<?> bytes = Bytes.from(yaml);
        TextWire wire = new TextWire(bytes);

        int value = wire.read("val").int32();
        assertEquals(0, value, "0x0 should parse as 0");
    }

    // ========== Negative Number Edge Cases ==========

    @Test
    @DisplayName("All wire types should handle negative numbers correctly")
    public void testNegativeNumbers() {
        long[] negatives = {-1, -127, -128, -255, -256, -32767, -32768, -65535, -65536};

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            for (long neg : negatives) {
                Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                Wire wire = wt.apply(bytes);

                wire.write("val").int64(neg);
                bytes.readPosition(0);

                assertEquals(neg, wire.read("val").int64(),
                        "Negative value " + neg + " should round-trip in " + wt);
            }
        }
    }

    // ========== Power of Two Boundaries ==========

    @Test
    @DisplayName("All wire types should handle powers of 2 boundaries")
    public void testPowerOfTwoBoundaries() {
        // Powers of 2 from 2^0 to 2^62
        for (int exp = 0; exp <= 62; exp++) {
            long power = 1L << exp;

            for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
                Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                Wire wire = wt.apply(bytes);

                wire.write("pow").int64(power);
                wire.write("powMinus1").int64(power - 1);
                wire.write("powPlus1").int64(power + 1);

                bytes.readPosition(0);

                assertEquals(power, wire.read("pow").int64(),
                        "2^" + exp + " should round-trip in " + wt);
                assertEquals(power - 1, wire.read("powMinus1").int64(),
                        "2^" + exp + " - 1 should round-trip in " + wt);
                assertEquals(power + 1, wire.read("powPlus1").int64(),
                        "2^" + exp + " + 1 should round-trip in " + wt);
            }
        }
    }

    // ========== Signed vs Unsigned Interpretation ==========

    @Test
    @DisplayName("BinaryWire should preserve high-bit long values")
    public void testHighBitLongValues() {
        // Values with high bit set (would be negative if signed)
        long[] highBits = {
                0x8000000000000000L,  // Long.MIN_VALUE
                0xFFFFFFFFFFFFFFFFL,  // -1 as unsigned
                0x8000000000000001L,
                0xFFFFFFFFFFFFFFFEL   // -2 as unsigned
        };

        for (long val : highBits) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            BinaryWire wire = new BinaryWire(bytes);

            wire.write("val").int64(val);
            bytes.readPosition(0);

            assertEquals(val, wire.read("val").int64(),
                    "High-bit value 0x" + Long.toHexString(val) + " should round-trip");
        }
    }

    // ========== Zero Value Tests ==========

    @Test
    @DisplayName("All wire types should handle zero correctly")
    public void testZeroValues() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("int8").int8((byte) 0);
            wire.write("int16").int16((short) 0);
            wire.write("int32").int32(0);
            wire.write("int64").int64(0L);

            bytes.readPosition(0);

            assertEquals(0, wire.read("int8").int8(), "Zero int8 in " + wt);
            assertEquals(0, wire.read("int16").int16(), "Zero int16 in " + wt);
            assertEquals(0, wire.read("int32").int32(), "Zero int32 in " + wt);
            assertEquals(0L, wire.read("int64").int64(), "Zero int64 in " + wt);
        }
    }

    // ========== Type Narrowing Tests ==========

    @Test
    @DisplayName("Reading wider type as narrower should work within range")
    public void testTypeNarrowingWithinRange() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // Write as long, read as int (within int range)
        wire.write("val").int64(42L);

        bytes.readPosition(0);

        // Should work because 42 fits in int
        int result = wire.read("val").int32();
        assertEquals(42, result, "Long value within int range should read as int");
    }

    // ========== Boundary Transition Tests ==========

    @Test
    @DisplayName("Values at type boundary transitions should round-trip")
    public void testTypeBoundaryTransitions() {
        // Values at boundaries where encoding/representation might change
        long[] boundaries = {
                127, 128,        // 1-byte to 2-byte
                255, 256,        // unsigned byte max
                32767, 32768,    // short max
                65535, 65536,    // unsigned short max
                2147483647L, 2147483648L  // int max
        };

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            for (long boundary : boundaries) {
                Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                Wire wire = wt.apply(bytes);

                wire.write("val").int64(boundary);
                bytes.readPosition(0);

                assertEquals(boundary, wire.read("val").int64(),
                        "Boundary " + boundary + " should round-trip in " + wt);
            }
        }
    }
}
