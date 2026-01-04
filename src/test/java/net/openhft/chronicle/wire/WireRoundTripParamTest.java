/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic round‑trip checks across a few {@link WireType}s.
 * Covers primitive scalars and a small sequence to exercise ValueIn/ValueOut
 * for Binary/Text/YAML wires.
 */
public class WireRoundTripParamTest extends WireTestCommon {

    private static final WireType[] TYPES = {
            WireType.BINARY,
            WireType.TEXT,
            WireType.YAML
    };

    @Test
    @DisplayName("Primitive scalars round trip across wire types")
    public void primitivesRoundTrip() {
        for (WireType wt : TYPES) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            w.write("i8").int8((byte) -1);
            w.write("i16").int16((short) 32767);
            w.write("i32").int32(123456789);
            w.write("i64").int64(Long.MIN_VALUE + 1);
            w.write("fp").float32(3.25f);
            w.write("dp").float64(Math.PI);
            w.write("b").bool(true);
            w.write("txt").text("hello");

            // read back
            assertEquals((byte) -1,
                    w.read("i8").int8(),
                    "i8 should round trip for " + wt);
            assertEquals(32767,
                    w.read("i16").int16(),
                    "i16 should round trip for " + wt);
            assertEquals(123456789,
                    w.read("i32").int32(),
                    "i32 should round trip for " + wt);
            assertEquals(Long.MIN_VALUE + 1,
                    w.read("i64").int64(),
                    "i64 should round trip for " + wt);
            assertEquals(3.25f,
                    w.read("fp").float32(),
                    0.0f,
                    "Float32 should round trip for " + wt);
            assertEquals(Math.PI,
                    w.read("dp").float64(),
                    0.0,
                    "Float64 should round trip for " + wt);
            assertTrue(w.read("b").bool(),
                    "Boolean should round trip for " + wt);
            assertEquals("hello",
                    w.read("txt").text(),
                    "Text should round trip for " + wt);
        }
    }

    @Test
    @DisplayName("Sequences round trip across wire types")
    public void sequenceRoundTrip() {
        for (WireType wt : TYPES) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            w.write("seq").sequence(v -> {
                v.int32(1);
                v.text("two");
                v.int64(3L);
            });

            final Object[] out = new Object[3];
            w.read("seq").sequence(out, (arr, in) -> {
                arr[0] = in.int32();
                arr[1] = in.text();
                arr[2] = in.int64();
            });

            assertArrayEquals(new Object[]{1, "two", 3L},
                    out,
                    "Sequence should round trip for " + wt);
        }
    }
}
