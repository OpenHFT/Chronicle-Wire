/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip tests for milli/micro/nano timestamp converters using append/parse.
 */
public class ConvertersEdgeTest extends WireTestCommon {

    private static long roundTrip(LongConverter c, long v) {
        StringBuilder sb = new StringBuilder();
        c.append(sb, v);
        long out = c.parse(sb);
        assertEquals(v, out,
                "append/parse should preserve value for converter " + c.getClass().getSimpleName() + ", input=" + v);

        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        c.append(b, v);
        String s = b.toString();
        long out2 = c.parse(s, 0, s.length());
        assertEquals(v, out2,
                "bytes append/parse should preserve value for converter " + c.getClass().getSimpleName() + ", input=" + v);
        return out2;
    }

    @Test
    @DisplayName("Round-trips milli, micro, and nano converters across ranges")
    public void milliMicroNanoRoundTrips() {
        // choose values across ranges
        long nowMs = System.currentTimeMillis();
        long nowUs = nowMs * 1000L + 321;
        long nowNs = nowMs * 1_000_000L + 654_321;

        assertEquals(nowMs, roundTrip(MilliTimestampLongConverter.INSTANCE, nowMs),
                "milli timestamp round-trip should preserve value, nowMs=" + nowMs);
        roundTrip(MicroTimestampLongConverter.INSTANCE, nowUs);
        roundTrip(NanoTimestampLongConverter.INSTANCE, nowNs);

        // zero and negative
        roundTrip(MilliTimestampLongConverter.INSTANCE, 0L);
        roundTrip(MicroTimestampLongConverter.INSTANCE, -1L);
        roundTrip(NanoTimestampLongConverter.INSTANCE, 1L);
    }
}
