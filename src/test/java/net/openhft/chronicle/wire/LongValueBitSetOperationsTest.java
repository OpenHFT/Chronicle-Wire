//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 * Exercises {@link LongValueBitSet} across word boundaries and common operations.
 */
public class LongValueBitSetOperationsTest extends WireTestCommon {

    private static LongValueBitSet newSet(int bits) {
        // Bind to a BinaryWire so LongReferences are initialised
        final Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
        return new LongValueBitSet(bits, w);
    }

    @Test
    public void setGetFlipAcrossWords() {
        LongValueBitSet bs = newSet(128);
        try {

            // Set boundary bits
            bs.set(0);
            bs.set(63);
            bs.set(64);
            bs.set(127);

            assertTrue(bs.get(0));
            assertTrue(bs.get(63));
            assertTrue(bs.get(64));
            assertTrue(bs.get(127));

            assertEquals(0, bs.nextSetBit(0));
            assertEquals(63, bs.nextSetBit(1));
            assertEquals(64, bs.nextSetBit(64));
            assertEquals(127, bs.nextSetBit(66));

            // Flip a range spanning words
            bs.flip(60, 68);
            assertFalse(bs.get(63));
            assertFalse(bs.get(64));
            assertTrue(bs.get(60));
            assertTrue(bs.get(67));

            // Clear range that includes last bit
            bs.clear(120, 128);
            assertEquals(-1, bs.nextSetBit(120));
        } finally {
            bs.close();
        }
    }

    @Test
    public void cardinalityAndToByteArray() {
        LongValueBitSet bs = newSet(130);
        try {
            bs.set(1);
            bs.set(65);
            bs.set(129);
            assertEquals(3, bs.cardinality());
            byte[] arr = bs.toByteArray();
            assertNotNull(arr);
            assertTrue(arr.length > 0);
        } finally {
            bs.close();
        }
    }
}
