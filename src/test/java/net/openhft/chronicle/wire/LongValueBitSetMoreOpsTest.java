/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
class LongValueBitSetMoreOpsTest extends WireTestCommon {

    private static LongValueBitSet bound(int bits) {
        return new LongValueBitSet(bits, new BinaryWire(Bytes.allocateElasticOnHeap(256)));
    }

    @Test
    @DisplayName("Finds previous and next clear bits")
    void previousAndNextClearBits() {
        try (LongValueBitSet bs = bound(256)) {
            bs.set(1);
            bs.set(63);
            bs.set(64);
            bs.set(200);
            assertEquals(0, bs.nextClearBit(0),
                    "next clear bit from 0 should be 0");
            assertEquals(62, bs.previousClearBit(63),
                    "previous clear bit from 63 should be 62");
            assertEquals(65, bs.nextClearBit(65),
                    "next clear bit from 65 should be 65");
            assertEquals(199, bs.previousClearBit(200),
                    "previous clear bit from 200 should be 199");
        }
    }

    @Test
    @DisplayName("Streams and marshallable round-trip bitsets")
    void streamEqualsCopyFromAndMarshallRoundTrip() {
        try (LongValueBitSet a = bound(128); LongValueBitSet b = bound(128)) {
            a.set(3);
            a.set(5);
            a.set(127);
            b.copyFrom(a);
            assertEquals(a, b,
                    "copyFrom should produce identical bitset");
            assertTrue(a.stream().anyMatch(i -> i == 3),
                    "stream should include set bit 3");

            Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
            w.write("bs").object(a);
            LongValueBitSet r = w.read("bs").object(LongValueBitSet.class);
            assertEquals(a, r,
                    "bitset should round-trip through wire marshalling");
            r.close();
        }
    }
}
