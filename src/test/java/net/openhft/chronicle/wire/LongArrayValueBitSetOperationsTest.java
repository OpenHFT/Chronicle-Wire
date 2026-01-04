/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link LongArrayValueBitSet} operations across multiple words to validate set, clear, and range behaviour.
 */
class LongArrayValueBitSetOperationsTest extends WireTestCommon {

    @Test
    @DisplayName("Performs basic set and range operations")
    void basicOps() {
        try (LongArrayValueBitSet bs = new LongArrayValueBitSet(192, new BinaryWire(Bytes.allocateElasticOnHeap(256)))) {
            bs.set(0);
            bs.set(64);
            bs.set(128);

            assertTrue(bs.get(0), "bit 0 should be set");
            assertTrue(bs.get(64), "bit 64 should be set");
            assertTrue(bs.get(128), "bit 128 should be set");

            assertEquals(0, bs.nextSetBit(0),
                    "next set bit from 0 should be 0");
            assertEquals(64, bs.nextSetBit(1),
                    "next set bit from 1 should be 64");
            assertEquals(128, bs.nextSetBit(65),
                    "next set bit from 65 should be 128");

            bs.clear(64);
            assertFalse(bs.get(64), "bit 64 should be cleared");
            assertEquals(2, bs.cardinality(),
                    "cardinality should reflect cleared bit");

            // Range operations
            bs.set(10, 15);
            for (int i = 10; i < 15; i++)
                assertTrue(bs.get(i), "range set should include bit " + i);
            bs.clear(12, 14);
            assertTrue(bs.get(10), "range clear should preserve bit 10");
            assertFalse(bs.get(12), "range clear should unset bit 12");
            assertTrue(bs.get(14), "range clear should keep end bit 14");
        }
    }
}
