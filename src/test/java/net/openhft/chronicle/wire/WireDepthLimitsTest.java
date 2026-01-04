/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for depth limits, size limits, and algorithmic complexity scenarios.
 * Verifies behaviour with deeply nested structures and large data sets.
 */
@SuppressWarnings({"deprecation", "removal", "unchecked"})
public class WireDepthLimitsTest extends WireTestCommon {

    // ========== Deep Nesting Tests ==========

    @Test
    @DisplayName("BinaryWire should handle moderately deep nesting (10 levels)")
    public void testModerateNestingBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(1024);
        BinaryWire wire = new BinaryWire(bytes);
        wire.usePadding(false);

        // Write 10 levels of nesting
        writeNestedMarshallable(wire, 10, "leaf");

        bytes.readPosition(0);

        // Read back and verify
        String result = readNestedMarshallable(wire, 10);
        assertEquals("leaf", result, "10-level nested value should round-trip");
    }

    @Test
    @DisplayName("BinaryWire should handle deep nesting (50 levels)")
    public void testDeepNestingBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(4096);
        BinaryWire wire = new BinaryWire(bytes);
        wire.usePadding(false);

        writeNestedMarshallable(wire, 50, "deep");

        bytes.readPosition(0);

        String result = readNestedMarshallable(wire, 50);
        assertEquals("deep", result, "50-level nested value should round-trip");
    }

    @Test
    @DisplayName("BinaryWire should handle very deep nesting (100 levels)")
    public void testVeryDeepNestingBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(8192);
        BinaryWire wire = new BinaryWire(bytes);
        wire.usePadding(false);

        try {
            writeNestedMarshallable(wire, 100, "verydeep");

            bytes.readPosition(0);

            String result = readNestedMarshallable(wire, 100);
            assertEquals("verydeep", result, "100-level nested value should round-trip");
        } catch (StackOverflowError e) {
            fail("100-level nesting should not cause StackOverflowError");
        }
    }

    // ========== Large Sequence Tests ==========

    @Test
    @DisplayName("BinaryWire should handle large sequence (1000 items)")
    public void testLargeSequenceBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32768);
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("items").sequence(s -> {
            for (int i = 0; i < 1000; i++) {
                s.int32(i);
            }
        });

        bytes.readPosition(0);

        List<Integer> result = new ArrayList<>();
        wire.read("items").sequence(result, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.int32());
            }
        });

        assertEquals(1000, result.size(), "Binary sequence read should return 1000 items");
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, result.get(i), "Item " + i + " should match");
        }
    }

    @Test
    @DisplayName("BinaryWire should handle large sequence (10000 items)")
    public void testVeryLargeSequenceBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(131072);
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("items").sequence(s -> {
            for (int i = 0; i < 10000; i++) {
                s.int32(i);
            }
        });

        bytes.readPosition(0);

        List<Integer> result = new ArrayList<>();
        wire.read("items").sequence(result, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.int32());
            }
        });

        assertEquals(10000, result.size(), "Binary sequence read should return 10000 items");
    }

    // ========== Large Map Tests ==========

    @Test
    @DisplayName("BinaryWire should handle large map (1000 entries)")
    public void testLargeMapBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(65536);
        BinaryWire wire = new BinaryWire(bytes);

        // Write 1000 key-value pairs
        wire.write("data").marshallable(w -> {
            for (int i = 0; i < 1000; i++) {
                w.write("key" + i).int32(i);
            }
        });

        bytes.readPosition(0);

        // Read back
        Map<String, Integer> result = new HashMap<>();
        wire.read("data").marshallable(w -> {
            for (int i = 0; i < 1000; i++) {
                result.put("key" + i, w.read("key" + i).int32());
            }
        });

        assertEquals(1000, result.size(), "Binary map read should return 1000 entries");
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, result.get("key" + i), "Value for key" + i + " should match");
        }
    }

    // ========== Long Key Tests ==========

    @Test
    @DisplayName("BinaryWire should handle very long field names")
    public void testVeryLongFieldNames() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(4096);
        BinaryWire wire = new BinaryWire(bytes);

        // Create field names of increasing lengths
        String name100 = repeat('x', 100);
        String name500 = repeat('y', 500);
        String name1000 = repeat('z', 1000);

        wire.write(name100).int32(100);
        wire.write(name500).int32(500);
        wire.write(name1000).int32(1000);

        bytes.readPosition(0);

        assertEquals(100, wire.read(name100).int32(),
                "100-char field name should work");
        assertEquals(500, wire.read(name500).int32(),
                "500-char field name should work");
        assertEquals(1000, wire.read(name1000).int32(),
                "1000-char field name should work");
    }

    // ========== Long Value Tests ==========

    @Test
    @DisplayName("BinaryWire should handle very long string values")
    public void testVeryLongStringValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(1024 * 1024);
        BinaryWire wire = new BinaryWire(bytes);

        String value10k = repeat('a', 10000);
        String value100k = repeat('b', 100000);

        wire.write("small").text(value10k);
        wire.write("large").text(value100k);

        bytes.readPosition(0);

        assertEquals(value10k, wire.read("small").text(),
                "10K string should round-trip");
        assertEquals(value100k, wire.read("large").text(),
                "100K string should round-trip");
    }

    // ========== Nested Sequence Tests ==========

    @Test
    @DisplayName("BinaryWire should handle nested sequence structures")
    public void testNestedSequences() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(4096);
        BinaryWire wire = new BinaryWire(bytes);

        // Write sequence of sequences
        wire.write("matrix").sequence(w -> {
            for (int row = 0; row < 10; row++) {
                final int r = row;
                w.sequence(inner -> {
                    for (int col = 0; col < 10; col++) {
                        inner.int32(r * 10 + col);
                    }
                });
            }
        });

        bytes.readPosition(0);

        // Read back
        List<List<Integer>> matrix = new ArrayList<>();
        wire.read("matrix").sequence(matrix, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                List<Integer> row = new ArrayList<>();
                v.sequence(row, (innerList, innerV) -> {
                    while (innerV.hasNextSequenceItem()) {
                        innerList.add(innerV.int32());
                    }
                });
                list.add(row);
            }
        });

        assertEquals(10, matrix.size(), "Matrix should contain 10 rows");
        for (int row = 0; row < 10; row++) {
            assertEquals(10, matrix.get(row).size(), "Row " + row + " should have 10 columns");
            for (int col = 0; col < 10; col++) {
                assertEquals(row * 10 + col, matrix.get(row).get(col),
                        "Value at [" + row + "][" + col + "] should match");
            }
        }
    }

    // ========== Many Fields Tests ==========

    @Test
    @DisplayName("BinaryWire should handle object with many fields")
    public void testManyFields() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(65536);
        BinaryWire wire = new BinaryWire(bytes);

        // Write 500 fields with various types
        wire.write("obj").marshallable(w -> {
            for (int i = 0; i < 100; i++) {
                w.write("int" + i).int32(i);
                w.write("long" + i).int64(i * 1000L);
                w.write("double" + i).float64(i + 0.5);
                w.write("string" + i).text("value" + i);
                w.write("bool" + i).bool(i % 2 == 0);
            }
        });

        bytes.readPosition(0);

        // Read back and verify
        wire.read("obj").marshallable(w -> {
            for (int i = 0; i < 100; i++) {
                assertEquals(i, w.read("int" + i).int32(),
                        "Many-fields int value should match for index " + i);
                assertEquals(i * 1000L, w.read("long" + i).int64(),
                        "Many-fields long value should match for index " + i);
                assertEquals(i + 0.5, w.read("double" + i).float64(), 0.001,
                        "Many-fields double value should match for index " + i);
                assertEquals("value" + i, w.read("string" + i).text(),
                        "Many-fields string value should match for index " + i);
                assertEquals(i % 2 == 0, w.read("bool" + i).bool(),
                        "Many-fields boolean value should match for index " + i);
            }
        });
    }

    // ========== Text Wire Deep Nesting Tests ==========

    @Test
    @DisplayName("TextWire should handle deep nesting (20 levels)")
    public void testDeepNestingText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(4096);
        TextWire wire = new TextWire(bytes);

        writeNestedMarshallable(wire, 20, "textdeep");

        bytes.readPosition(0);

        String result = readNestedMarshallable(wire, 20);
        assertEquals("textdeep", result, "20-level nested value should round-trip in TextWire");
    }

    @Test
    @DisplayName("YamlWire should handle deep nesting (20 levels)")
    public void testDeepNestingYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(4096);
        YamlWire wire = new YamlWire(bytes);

        writeNestedMarshallable(wire, 20, "yamldeep");

        bytes.readPosition(0);

        String result = readNestedMarshallable(wire, 20);
        assertEquals("yamldeep", result, "20-level nested value should round-trip in YamlWire");
    }

    // ========== Mixed Deep Structure Tests ==========

    @Test
    @DisplayName("BinaryWire should handle mixed deep structures")
    public void testMixedDeepStructures() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16384);
        BinaryWire wire = new BinaryWire(bytes);

        // Create a structure with both nesting and sequences
        wire.write("root").marshallable(w1 -> w1.write("items").sequence(s1 -> {
            for (int i = 0; i < 10; i++) {
                final int ii = i;
                s1.marshallable(w2 -> {
                    w2.write("id").int32(ii);
                    w2.write("children").sequence(s2 -> {
                        for (int j = 0; j < 5; j++) {
                            final int jj = j;
                            s2.marshallable(w3 -> {
                                w3.write("childId").int32(ii * 100 + jj);
                                w3.write("name").text("child_" + ii + "_" + jj);
                            });
                        }
                    });
                });
            }
        }));

        bytes.readPosition(0);

        // Read back and verify structure
        final int[] iCounter = {0};
        wire.read("root").marshallable(w1 -> {
            List<Object> itemsDummy = new ArrayList<>();
            w1.read("items").sequence(itemsDummy, (dummy1, s1) -> {
                while (s1.hasNextSequenceItem()) {
                    final int ii = iCounter[0];
                    s1.marshallable(w2 -> {
                        assertEquals(ii, w2.read("id").int32(),
                                "Mixed depth item id should match for i=" + ii);
                        final int[] jCounter = {0};
                        List<Object> childrenDummy = new ArrayList<>();
                        w2.read("children").sequence(childrenDummy, (dummy2, s2) -> {
                            while (s2.hasNextSequenceItem()) {
                                final int jj = jCounter[0];
                                s2.marshallable(w3 -> {
                                    assertEquals(ii * 100 + jj, w3.read("childId").int32(),
                                            "Mixed depth childId should match for i=" + ii + " j=" + jj);
                                    assertEquals("child_" + ii + "_" + jj, w3.read("name").text(),
                                            "Mixed depth child name should match for i=" + ii + " j=" + jj);
                                });
                                jCounter[0]++;
                            }
                            assertEquals(5, jCounter[0],
                                    "Mixed depth child list should contain 5 entries");
                        });
                    });
                    iCounter[0]++;
                }
                assertEquals(10, iCounter[0],
                        "Mixed depth root list should contain 10 entries");
            });
        });
    }

    // ========== Helper Methods ==========

    private void writeNestedMarshallable(WireOut wire, int depth, String leafValue) {
        if (depth <= 0) {
            wire.write("value").text(leafValue);
        } else {
            wire.write("level" + depth).marshallable(w -> writeNestedMarshallable(w, depth - 1, leafValue));
        }
    }

    private String readNestedMarshallable(WireIn wire, int depth) {
        if (depth <= 0) {
            return wire.read("value").text();
        } else {
            final String[] result = {null};
            wire.read("level" + depth).marshallable(w -> result[0] = readNestedMarshallable(w, depth - 1));
            return result[0];
        }
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
