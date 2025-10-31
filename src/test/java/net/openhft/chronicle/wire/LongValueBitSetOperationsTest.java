/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Exercises {@link LongValueBitSet} across word boundaries and common operations.
 */
public class LongValueBitSetOperationsTest extends WireTestCommon {

    private static LongValueBitSet newSet(int bits) {
        // Bind to a BinaryWire so LongReferences are initialised
        final Bytes<ByteBuffer> bytes = Bytes.allocateElasticOnHeap(256);
        final Wire w = new BinaryWire(bytes);
        return new LongValueBitSet(bits, w);
    }

    @Test
    public void setGetFlipAcrossWords() {
        LongValueBitSet bs = newSet(128);

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
    }

    @Test
    public void cardinalityAndToByteArray() {
        LongValueBitSet bs = newSet(130);
        bs.set(1);
        bs.set(65);
        bs.set(129);
        assertEquals(3, bs.cardinality());
        byte[] arr = bs.toByteArray();
        assertNotNull(arr);
        assertTrue(arr.length > 0);
    }
}

