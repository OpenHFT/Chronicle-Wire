//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Round-trip tests for milli/micro/nano timestamp converters using append/parse.
 */
public class ConvertersEdgeTest extends WireTestCommon {

    private static long roundTrip(LongConverter c, long v) {
        StringBuilder sb = new StringBuilder();
        c.append(sb, v);
        long out = c.parse(sb);
        assertEquals(v, out);

        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        c.append(b, v);
        String s = b.toString();
        long out2 = c.parse(s, 0, s.length());
        assertEquals(v, out2);
        return out2;
    }

    @Test
    public void milliMicroNanoRoundTrips() {
        // choose values across ranges
        long nowMs = System.currentTimeMillis();
        long nowUs = nowMs * 1000L + 321;
        long nowNs = nowMs * 1_000_000L + 654_321;

        roundTrip(MilliTimestampLongConverter.INSTANCE, nowMs);
        roundTrip(MicroTimestampLongConverter.INSTANCE, nowUs);
        roundTrip(NanoTimestampLongConverter.INSTANCE, nowNs);

        // zero and negative
        roundTrip(MilliTimestampLongConverter.INSTANCE, 0L);
        roundTrip(MicroTimestampLongConverter.INSTANCE, -1L);
        roundTrip(NanoTimestampLongConverter.INSTANCE, 1L);
    }
}

