/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for buffer boundaries, elastic resize, and memory edge cases.
 * These tests verify correct behaviour when data spans buffer boundaries
 * or triggers buffer expansion.
 */
@SuppressWarnings({"deprecation", "removal"})
class WireBufferBoundaryTest extends WireTestCommon {

    // ========== Elastic Buffer Resize Tests ==========

    @Test
    @DisplayName("BinaryWire should handle elastic resize during write")
    void testElasticResizeDuringWriteBinary() {
        // Start with small buffer, write more data than initial capacity
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        BinaryWire wire = new BinaryWire(bytes);

        // Write 96 bytes of payload to force resize
        String largeValue = repeat('x', 96);
        wire.write("key").text(largeValue);

        bytes.readPosition(0);
        String result = wire.read("key").text();
        assertEquals(largeValue, result, "Large value should survive buffer resize");
    }

    @Test
    @DisplayName("TextWire should handle elastic resize during write")
    void testElasticResizeDuringWriteText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        TextWire wire = new TextWire(bytes);

        String largeValue = repeat('y', 96);
        wire.write("key").text(largeValue);

        bytes.readPosition(0);
        String result = wire.read("key").text();
        assertEquals(largeValue, result, "Large value should survive buffer resize in TextWire");
    }

    @Test
    @DisplayName("YamlWire should handle elastic resize during write")
    void testElasticResizeDuringWriteYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        YamlWire wire = new YamlWire(bytes);

        String largeValue = repeat('z', 96);
        wire.write("key").text(largeValue);

        bytes.readPosition(0);
        String result = wire.read("key").text();
        assertEquals(largeValue, result, "Large value should survive buffer resize in YamlWire");
    }

    // ========== Large Data Tests ==========

    @Test
    @DisplayName("BinaryWire should handle strings exceeding 16-bit length prefix (>64KB)")
    void testLargeStringBeyond64KB() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(70000);
        BinaryWire wire = new BinaryWire(bytes);

        // Create string larger than 65535 bytes
        String largeString = repeat('A', 65540);
        wire.write("big").text(largeString);

        bytes.readPosition(0);
        String result = wire.read("big").text();
        assertEquals(largeString.length(), result.length(),
                "String >64KB should round-trip correctly");
        assertEquals(largeString, result, "String content should match");
    }

    @Test
    @DisplayName("All wire types should handle many small fields")
    void testManySmallFields() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
            Wire wire = wt.apply(bytes);

            // Write many fields, forcing multiple resizes
            int fieldCount = 1000;
            for (int i = 0; i < fieldCount; i++) {
                wire.write("f" + i).int32(i);
            }

            bytes.readPosition(0);

            // Verify all fields can be read back
            for (int i = 0; i < fieldCount; i++) {
                assertEquals(i, wire.read("f" + i).int32(),
                        "Field " + i + " should round-trip in " + wt);
            }
        }
    }

    // ========== Zero-Length Field Tests ==========

    @Test
    @DisplayName("BinaryWire should handle zero-length string")
    void testZeroLengthStringBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("empty").text("");
        bytes.readPosition(0);

        String result = wire.read("empty").text();
        assertEquals("", result, "Empty string should round-trip in BinaryWire");
    }

    @Test
    @DisplayName("TextWire should handle zero-length string")
    void testZeroLengthStringText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        wire.write("empty").text("");
        bytes.readPosition(0);

        String result = wire.read("empty").text();
        assertNotNull(result, "Empty string should not be null in TextWire");
        assertEquals("", result, "Empty string should round-trip in TextWire");
    }

    @Test
    @DisplayName("YamlWire should handle zero-length string")
    void testZeroLengthStringYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        wire.write("empty").text("");
        bytes.readPosition(0);

        String result = wire.read("empty").text();
        assertNotNull(result, "Empty string should not be null in YamlWire");
        assertEquals("", result, "Empty string should round-trip in YamlWire");
    }

    @Test
    @DisplayName("All wire types should handle zero-length bytes")
    void testZeroLengthBytes() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            byte[] emptyBytes = new byte[0];
            wire.write("empty").bytes(emptyBytes);

            bytes.readPosition(0);

            byte[] result = wire.read("empty").bytes();
            assertNotNull(result, "Empty bytes should not be null in " + wt);
            assertEquals(0, result.length, "Empty bytes length should be 0 in " + wt);
        }
    }

    // ========== Position Preservation Tests ==========

    @Test
    @DisplayName("Position should be preserved after elastic resize")
    void testPositionPreservationAfterResize() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        BinaryWire wire = new BinaryWire(bytes);

        // Write some initial data
        wire.write("small").int32(42);
        long positionAfterSmall = bytes.writePosition();

        // Force a resize
        String largeValue = repeat('x', 100);
        wire.write("large").text(largeValue);

        // Position should have advanced correctly
        assertTrue(bytes.writePosition() > positionAfterSmall,
                "Write position should advance after resize");

        // Reading should still work
        bytes.readPosition(0);
        assertEquals(42, wire.read("small").int32(),
                "Small int should read back after resize");
        assertEquals(largeValue, wire.read("large").text(),
                "Large value should read back after resize");
    }

    // ========== Multi-byte UTF-8 at Buffer Boundary Tests ==========

    @Test
    @DisplayName("BinaryWire should handle 4-byte UTF-8 character")
    void testFourByteUtf8Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // Pile of poo emoji - 4 bytes in UTF-8
        String emoji = "\uD83D\uDCA9";
        wire.write("emoji").text(emoji);

        bytes.readPosition(0);
        String result = wire.read("emoji").text();
        assertEquals(emoji, result, "4-byte UTF-8 emoji should round-trip in BinaryWire");
    }

    @Test
    @DisplayName("TextWire should handle 4-byte UTF-8 character")
    void testFourByteUtf8Text() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        String emoji = "\uD83D\uDCA9";
        wire.write("emoji").text(emoji);

        bytes.readPosition(0);
        String result = wire.read("emoji").text();
        assertEquals(emoji, result, "4-byte UTF-8 emoji should round-trip in TextWire");
    }

    @Test
    @DisplayName("YamlWire should handle 4-byte UTF-8 character")
    void testFourByteUtf8Yaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        String emoji = "\uD83D\uDCA9";
        wire.write("emoji").text(emoji);

        bytes.readPosition(0);
        String result = wire.read("emoji").text();
        assertEquals(emoji, result, "4-byte UTF-8 emoji should round-trip in YamlWire");
    }

    // ========== Exactly-Full Buffer Tests ==========

    @Test
    @DisplayName("Write that exactly fills initial buffer should work")
    void testExactlyFullBuffer() {
        // This test verifies behaviour when write exactly fills the buffer
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
            Wire wire = wt.apply(bytes);

            // Write incrementally until near capacity
            int count = 0;
            while (bytes.writePosition() < 50) {
                wire.write("k" + count).int32(count);
                count++;
            }

            bytes.readPosition(0);

            // Verify we can read back
            for (int i = 0; i < count; i++) {
                assertEquals(i, wire.read("k" + i).int32(),
                        "Field " + i + " should read back in " + wt);
            }
        }
    }

    // ========== Sequential Write/Read Tests ==========

    @Test
    @DisplayName("Sequential writes followed by sequential reads should work")
    void testSequentialWriteRead() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
            Wire wire = wt.apply(bytes);

            // Write sequence
            for (int i = 0; i < 100; i++) {
                wire.write("n" + i).int64(i * 1000L);
            }

            bytes.readPosition(0);

            // Read sequence
            for (int i = 0; i < 100; i++) {
                long value = wire.read("n" + i).int64();
                assertEquals(i * 1000L, value, "Sequential value at " + i + " in " + wt);
            }
        }
    }

    // ========== Mixed Type Writes Near Boundary ==========

    @Test
    @DisplayName("Mixed type writes near buffer boundary should work")
    void testMixedTypesNearBoundary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);

        // Write a mix of types that should trigger resize
        wire.write("int").int32(42);
        wire.write("long").int64(Long.MAX_VALUE);
        wire.write("double").float64(3.14159);
        wire.write("string").text(repeat('s', 50));
        wire.write("bool").bool(true);

        bytes.readPosition(0);

        assertEquals(42, wire.read("int").int32(),
                "Mixed boundary int should read back as 42");
        assertEquals(Long.MAX_VALUE, wire.read("long").int64(),
                "Mixed boundary long should read back as Long.MAX_VALUE");
        assertEquals(3.14159, wire.read("double").float64(), 0.00001,
                "Mixed boundary double should round-trip with tolerance");
        assertEquals(repeat('s', 50), wire.read("string").text(),
                "Mixed boundary string should round-trip intact");
        assertTrue(wire.read("bool").bool(),
                "Mixed boundary boolean should read back as true");
    }

    // ========== Nested Structure Near Boundary ==========

    @Test
    @DisplayName("Nested structure near buffer boundary should work")
    void testNestedStructureNearBoundary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryWire wire = new BinaryWire(bytes);
        wire.usePadding(false);

        // Fill buffer partially
        wire.write("prefix").text(repeat('p', 30));

        // Then write a nested structure
        wire.write("nested").marshallable(w -> {
            w.write("a").int32(1);
            w.write("b").int32(2);
            w.write("c").text("nested value");
        });

        bytes.readPosition(0);

        assertEquals(repeat('p', 30), wire.read("prefix").text(),
                "Prefix text should round-trip before nested structure");
        wire.read("nested").marshallable(w -> {
            assertEquals(1, w.read("a").int32(),
                    "Nested field a should read back as 1");
            assertEquals(2, w.read("b").int32(),
                    "Nested field b should read back as 2");
            assertEquals("nested value", w.read("c").text(),
                    "Nested field c should read back the expected text");
        });
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
