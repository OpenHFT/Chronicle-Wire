/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for stop-bit encoding boundary values.
 * Stop-bit encoding uses the high bit of each byte to indicate continuation.
 * These boundaries are critical for binary format correctness.
 */
@SuppressWarnings({"deprecation", "removal"})
public class WireStopBitBoundaryTest extends WireTestCommon {

    // Stop-bit encoding boundaries
    // 1 byte: 0 to 127 (0x00 to 0x7F)
    // 2 bytes: 128 to 16,383 (0x80 to 0x3FFF)
    // 3 bytes: 16,384 to 2,097,151 (0x4000 to 0x1FFFFF)
    // 4 bytes: 2,097,152 to 268,435,455 (0x200000 to 0x0FFFFFFF)
    // 5+ bytes: larger values

    private static final long[] UNSIGNED_BOUNDARIES = {
            0L,           // minimum
            127L,         // 1-byte max
            128L,         // 2-byte start
            16383L,       // 2-byte max
            16384L,       // 3-byte start
            2097151L,     // 3-byte max
            2097152L,     // 4-byte start
            268435455L,   // 4-byte max
            268435456L,   // 5-byte start
            Long.MAX_VALUE // maximum positive long
    };

    // ========== BinaryWire Stop-Bit Tests ==========

    @Test
    @DisplayName("BinaryWire should handle all stop-bit encoding boundaries for int64")
    public void testStopBitBoundariesInt64Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        BinaryWire wire = new BinaryWire(bytes);

        for (long boundary : UNSIGNED_BOUNDARIES) {
            bytes.clear();

            // Test boundary value
            wire.write("val").int64(boundary);
            bytes.readPosition(0);
            assertEquals(boundary, wire.read("val").int64(),
                    "Boundary " + boundary + " should round-trip in BinaryWire");

            // Test boundary - 1 (unless at 0)
            if (boundary > 0) {
                bytes.clear();
                wire.write("val").int64(boundary - 1);
                bytes.readPosition(0);
                assertEquals(boundary - 1, wire.read("val").int64(),
                        "Boundary-1 " + (boundary - 1) + " should round-trip in BinaryWire");
            }

            // Test boundary + 1 (unless at MAX_VALUE)
            if (boundary < Long.MAX_VALUE) {
                bytes.clear();
                wire.write("val").int64(boundary + 1);
                bytes.readPosition(0);
                assertEquals(boundary + 1, wire.read("val").int64(),
                        "Boundary+1 " + (boundary + 1) + " should round-trip in BinaryWire");
            }
        }
    }

    @Test
    @DisplayName("BinaryWire should handle negative values at stop-bit boundaries")
    public void testStopBitBoundariesNegativeInt64Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        BinaryWire wire = new BinaryWire(bytes);

        // Negative values have different encoding
        long[] negativeBoundaries = {
                -1L,
                -128L,
                -129L,
                -16384L,
                -16385L,
                -2097152L,
                -2097153L,
                Long.MIN_VALUE + 1,
                Long.MIN_VALUE
        };

        for (long value : negativeBoundaries) {
            bytes.clear();
            wire.write("val").int64(value);
            bytes.readPosition(0);
            assertEquals(value, wire.read("val").int64(),
                    "Negative value " + value + " should round-trip in BinaryWire");
        }
    }

    @Test
    @DisplayName("BinaryWire should handle stop-bit boundaries for int32")
    public void testStopBitBoundariesInt32Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        BinaryWire wire = new BinaryWire(bytes);

        int[] boundaries = {
                0,
                127,
                128,
                16383,
                16384,
                2097151,
                2097152,
                Integer.MAX_VALUE
        };

        for (int boundary : boundaries) {
            bytes.clear();
            wire.write("val").int32(boundary);
            bytes.readPosition(0);
            assertEquals(boundary, wire.read("val").int32(),
                    "Int32 boundary " + boundary + " should round-trip in BinaryWire");
        }
    }

    @Test
    @DisplayName("BinaryWire should handle stop-bit boundaries for int16")
    public void testStopBitBoundariesInt16Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        BinaryWire wire = new BinaryWire(bytes);

        short[] boundaries = {
                0,
                127,
                128,
                16383,
                Short.MAX_VALUE
        };

        for (short boundary : boundaries) {
            bytes.clear();
            wire.write("val").int16(boundary);
            bytes.readPosition(0);
            assertEquals(boundary, wire.read("val").int16(),
                    "Int16 boundary " + boundary + " should round-trip in BinaryWire");
        }
    }

    // ========== TextWire Numeric Boundary Tests ==========

    @Test
    @DisplayName("TextWire should handle all numeric boundaries for int64")
    public void testNumericBoundariesInt64Text() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        TextWire wire = new TextWire(bytes);

        for (long boundary : UNSIGNED_BOUNDARIES) {
            bytes.clear();
            wire.write("val").int64(boundary);
            bytes.readPosition(0);
            assertEquals(boundary, wire.read("val").int64(),
                    "Boundary " + boundary + " should round-trip in TextWire");
        }
    }

    @Test
    @DisplayName("TextWire should handle negative boundary values")
    public void testNegativeBoundariesText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        TextWire wire = new TextWire(bytes);

        long[] negativeBoundaries = {
                -1L, -127L, -128L, -129L,
                -16383L, -16384L, -16385L,
                Long.MIN_VALUE + 1, Long.MIN_VALUE
        };

        for (long value : negativeBoundaries) {
            bytes.clear();
            wire.write("val").int64(value);
            bytes.readPosition(0);
            assertEquals(value, wire.read("val").int64(),
                    "Negative value " + value + " should round-trip in TextWire");
        }
    }

    // ========== YamlWire Numeric Boundary Tests ==========

    @Test
    @DisplayName("YamlWire should handle all numeric boundaries for int64")
    public void testNumericBoundariesInt64Yaml() {
        for (long boundary : UNSIGNED_BOUNDARIES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            YamlWire wire = new YamlWire(bytes);

            wire.write("val").int64(boundary);
            bytes.readPosition(0);
            assertEquals(boundary, wire.read("val").int64(),
                    "Boundary " + boundary + " should round-trip in YamlWire");
        }
    }

    @Test
    @DisplayName("YamlWire should handle negative boundary values")
    public void testNegativeBoundariesYaml() {
        long[] negativeBoundaries = {
                -1L, -127L, -128L, -129L,
                -16383L, -16384L, -16385L,
                Long.MIN_VALUE + 1, Long.MIN_VALUE
        };

        for (long value : negativeBoundaries) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
            YamlWire wire = new YamlWire(bytes);

            wire.write("val").int64(value);
            bytes.readPosition(0);
            assertEquals(value, wire.read("val").int64(),
                    "Negative value " + value + " should round-trip in YamlWire");
        }
    }

    // ========== Byte Count Verification Tests ==========

    @Test
    @DisplayName("BinaryWire should use efficient stop-bit encoding")
    public void testStopBitEncodingEfficiency() {
        // Verify that small values use fewer bytes than large values
        Bytes<?> bytes1 = Bytes.allocateElasticOnHeap(32);
        BinaryWire wire1 = new BinaryWire(bytes1);
        wire1.write("v").int64(127L);
        long size127 = bytes1.writePosition();

        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap(32);
        BinaryWire wire2 = new BinaryWire(bytes2);
        wire2.write("v").int64(128L);
        long size128 = bytes2.writePosition();

        // 128 should use more bytes than 127 due to stop-bit boundary
        assertTrue(size128 >= size127,
                "Stop-bit size should be monotonic: size128=" + size128 + " size127=" + size127);
    }

    @Test
    @DisplayName("BinaryWire should handle field name stop-bit boundaries")
    public void testFieldNameStopBitBoundaries() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        BinaryWire wire = new BinaryWire(bytes);

        // Field names at various lengths that cross stop-bit boundaries
        String name7 = "1234567";      // 7 chars
        String name127 = repeat('x', 127);  // At boundary
        String name128 = repeat('x', 128);  // Cross boundary

        wire.write(name7).int32(1);
        wire.write(name127).int32(2);
        wire.write(name128).int32(3);

        bytes.readPosition(0);

        assertEquals(1, wire.read(name7).int32(),
                "Field name length 7 should read value 1");
        assertEquals(2, wire.read(name127).int32(),
                "Field name length 127 should read value 2");
        assertEquals(3, wire.read(name128).int32(),
                "Field name length 128 should read value 3");
    }

    // ========== Unsigned Long Tests ==========

    @Test
    @DisplayName("BinaryWire should handle unsigned long values near boundaries")
    public void testUnsignedLongBoundaries() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);
        BinaryWire wire = new BinaryWire(bytes);

        // Values that would be negative if treated as signed
        long[] unsignedValues = {
                Long.MAX_VALUE,
                Long.MAX_VALUE - 1,
                0x7FFFFFFFFFFFFFFFL,
                0x7FFFFFFFFFFFFFFEL
        };

        for (long value : unsignedValues) {
            bytes.clear();
            wire.write("val").int64(value);
            bytes.readPosition(0);
            assertEquals(value, wire.read("val").int64(),
                    "Unsigned-range value " + Long.toUnsignedString(value) + " should round-trip");
        }
    }

    // ========== Cross-Wire-Type Boundary Tests ==========

    @Test
    @DisplayName("All wire types should handle power-of-two boundaries")
    public void testPowerOfTwoBoundaries() {
        // Powers of 2 are important boundaries for many encodings
        long[] powersOf2 = {
                1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L,
                256L, 512L, 1024L, 2048L, 4096L, 8192L, 16384L,
                32768L, 65536L, 131072L, 262144L, 524288L, 1048576L
        };

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            for (long power : powersOf2) {
                Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                Wire wire = wt.apply(bytes);

                wire.write("val").int64(power);
                wire.write("minus1").int64(power - 1);
                bytes.readPosition(0);

                assertEquals(power, wire.read("val").int64(),
                        "Power of 2 (" + power + ") should round-trip in " + wt);
                assertEquals(power - 1, wire.read("minus1").int64(),
                        "Power of 2 - 1 (" + (power - 1) + ") should round-trip in " + wt);
            }
        }
    }

    @Test
    @DisplayName("All wire types should handle 0xFF boundary values")
    public void testByteMaxBoundaries() {
        // Values at 0xFF boundaries (255, 256, etc.)
        long[] byteBoundaries = {
                255L, 256L,           // 1-byte unsigned max
                65535L, 65536L,       // 2-byte unsigned max
                16777215L, 16777216L  // 3-byte unsigned max
        };

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            for (long value : byteBoundaries) {
                Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                Wire wire = wt.apply(bytes);

                wire.write("val").int64(value);
                bytes.readPosition(0);

                assertEquals(value, wire.read("val").int64(),
                        "Byte boundary " + value + " should round-trip in " + wt);
            }
        }
    }

    // ========== Helper Methods ==========

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
