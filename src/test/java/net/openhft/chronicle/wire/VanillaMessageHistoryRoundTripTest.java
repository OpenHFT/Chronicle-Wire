/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaMessageHistoryRoundTripTest extends WireTestCommon {

    @Test
    @DisplayName("Round-trips message history through wire")
    void roundTripViaWire() {
        VanillaMessageHistory mh = new VanillaMessageHistory();
        mh.addSource(1, 11L);
        mh.addTiming(123L);

        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        w.write("mh").object(mh);
        VanillaMessageHistory r = w.read("mh").object(VanillaMessageHistory.class);

        // write adds one timing for the write itself
        assertEquals(mh.timings() + 1, r.timings(),
                "Expected round-trip timings to include write timing via wire");
        assertEquals(mh.timing(0), r.timing(0),
                "Expected first timing to round-trip via wire");
    }

    @Test
    @DisplayName("Round-trips message history through bytes")
    void roundTripViaBytes() {
        VanillaMessageHistory mh = new VanillaMessageHistory();
        mh.addSource(2, 22L);
        mh.addTiming(456L);

        Bytes<?> b = Bytes.allocateElasticOnHeap(128);
        mh.writeMarshallable(b);
        assertTrue(b.readRemaining() > 0, "Expected bytes after writing message history");

        VanillaMessageHistory r = new VanillaMessageHistory();
        r.readMarshallable(b);
        assertEquals(mh.timings() + 1, r.timings(),
                "Expected round-trip timings to include write timing via bytes");
        assertEquals(mh.timing(0), r.timing(0),
                "Expected first timing to round-trip via bytes");
    }
}
