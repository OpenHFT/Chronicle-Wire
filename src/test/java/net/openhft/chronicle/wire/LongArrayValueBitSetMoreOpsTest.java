/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
public class LongArrayValueBitSetMoreOpsTest extends WireTestCommon {

    @Test
    @DisplayName("Finds previous and next bits with ranges")
    public void previousAndRangeOps() {
        try (LongArrayValueBitSet bs = new LongArrayValueBitSet(256, new BinaryWire(Bytes.allocateElasticOnHeap(256)))) {
            bs.set(2, 70);
            assertTrue(bs.get(2), "range set should include bit 2");
            assertTrue(bs.get(69), "range set should include bit 69");
            assertEquals(69, bs.previousSetBit(127),
                    "previous set bit from 127 should be 69");

            bs.clear(10, 60);
            assertFalse(bs.get(10), "clear range should unset bit 10");
            assertEquals(60, bs.nextSetBit(10),
                    "next set bit from 10 should be 60");
        }
    }
}
