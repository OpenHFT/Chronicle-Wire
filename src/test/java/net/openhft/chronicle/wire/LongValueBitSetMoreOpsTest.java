//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

public class LongValueBitSetMoreOpsTest extends WireTestCommon {

    private static LongValueBitSet bound(int bits) {
        return new LongValueBitSet(bits, new BinaryWire(Bytes.allocateElasticOnHeap(256)));
    }

    @Test
    public void previousAndNextClearBits() {
        LongValueBitSet bs = bound(256);
        try {
            bs.set(1); bs.set(63); bs.set(64); bs.set(200);
            assertEquals(0, bs.nextClearBit(0));
            assertEquals(62, bs.previousClearBit(63));
            assertEquals(65, bs.nextClearBit(65));
            assertEquals(199, bs.previousClearBit(200));
        } finally {
            bs.close();
        }
    }

    @Test
    public void streamEqualsCopyFromAndMarshallRoundTrip() {
        LongValueBitSet a = bound(128);
        LongValueBitSet b = bound(128);
        try {
            a.set(3); a.set(5); a.set(127);
            b.copyFrom(a);
            assertEquals(a, b);
            assertTrue(a.stream().anyMatch(i -> i == 3));

            Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
            w.write("bs").object(a);
            LongValueBitSet r = w.read("bs").object(LongValueBitSet.class);
            assertEquals(a, r);
            r.close();
        } finally {
            a.close();
            b.close();
        }
    }
}

