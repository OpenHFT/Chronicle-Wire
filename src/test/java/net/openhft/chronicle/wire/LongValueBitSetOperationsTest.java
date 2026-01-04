/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link LongValueBitSet} across word boundaries to validate set, flip, clear, and sizing behaviour.
 */
@SuppressWarnings({"deprecation", "removal"})
public class LongValueBitSetOperationsTest extends WireTestCommon {

    private static LongValueBitSet newSet(int bits) {
        // Bind to a BinaryWire so LongReferences are initialised
        final Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
        return new LongValueBitSet(bits, w);
    }

    @Test
    @DisplayName("Sets, flips, and clears bits across words")
    public void setGetFlipAcrossWords() {
        try (LongValueBitSet bs = newSet(128)) {

            // Set boundary bits
            bs.set(0);
            bs.set(63);
            bs.set(64);
            bs.set(127);

            assertTrue(bs.get(0), "bit 0 should be set");
            assertTrue(bs.get(63), "bit 63 should be set");
            assertTrue(bs.get(64), "bit 64 should be set");
            assertTrue(bs.get(127), "bit 127 should be set");

            assertEquals(0, bs.nextSetBit(0),
                    "next set bit from 0 should be 0");
            assertEquals(63, bs.nextSetBit(1),
                    "next set bit from 1 should be 63");
            assertEquals(64, bs.nextSetBit(64),
                    "next set bit from 64 should be 64");
            assertEquals(127, bs.nextSetBit(66),
                    "next set bit from 66 should be 127");

            // Flip a range spanning words
            bs.flip(60, 68);
            assertFalse(bs.get(63), "flip range should clear bit 63");
            assertFalse(bs.get(64), "flip range should clear bit 64");
            assertTrue(bs.get(60), "flip range should set bit 60");
            assertTrue(bs.get(67), "flip range should set bit 67");

            // Clear range that includes last bit
            bs.clear(120, 128);
            assertEquals(-1, bs.nextSetBit(120),
                    "next set bit should be -1 after clearing tail range");
        }
    }

    @Test
    @DisplayName("Calculates cardinality and byte array form")
    public void cardinalityAndToByteArray() {
        try (LongValueBitSet bs = newSet(130)) {
            bs.set(1);
            bs.set(65);
            bs.set(129);
            assertEquals(3, bs.cardinality(),
                    "cardinality should count three set bits");
            byte[] arr = bs.toByteArray();
            assertNotNull(arr, "byte array should be allocated");
            assertTrue(arr.length > 0, "byte array should contain data, length=" + arr.length);
        }
    }
}
