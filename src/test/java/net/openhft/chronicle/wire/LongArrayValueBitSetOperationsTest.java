//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Exercises {@link LongArrayValueBitSet} operations across multiple words.
 */
public class LongArrayValueBitSetOperationsTest extends WireTestCommon {

    @Test
    public void basicOps() {
        LongArrayValueBitSet bs = new LongArrayValueBitSet(192, new BinaryWire(Bytes.allocateElasticOnHeap(256)));
        try {
            bs.set(0);
            bs.set(64);
            bs.set(128);

            assertTrue(bs.get(0));
            assertTrue(bs.get(64));
            assertTrue(bs.get(128));

            assertEquals(0, bs.nextSetBit(0));
            assertEquals(64, bs.nextSetBit(1));
            assertEquals(128, bs.nextSetBit(65));

            bs.clear(64);
            assertFalse(bs.get(64));
            assertEquals(2, bs.cardinality());

            // Range operations
            bs.set(10, 15);
            for (int i = 10; i < 15; i++) assertTrue(bs.get(i));
            bs.clear(12, 14);
            assertTrue(bs.get(10));
            assertFalse(bs.get(12));
            assertTrue(bs.get(14));
        } finally {
            bs.close();
        }
    }
}
