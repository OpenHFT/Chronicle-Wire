/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises ValueIn array readers using sequences for common wire types.
 */
class ValueInArrayReadersTest extends WireTestCommon {

    @Test
    void readDoubleArrayFromSequence() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));
            w.write("arr").sequence(v -> {
                v.float64(1.5);
                v.float64(-2.25);
                v.float64(3.0);
            });

            double[] out = new double[3];
            int n = w.read("arr").array(out);
            assertEquals(3, n);
            assertArrayEquals(new double[]{1.5, -2.25, 3.0}, out, 0.0);
        }
    }

    @Test
    void readIntArrayFromSequence() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));
            w.write("arr").sequence(v -> {
                v.int32(-1);
                v.int32(0);
                v.int32(7);
            });

            int[] out = new int[3];
            int n = w.read("arr").array(out);
            assertEquals(3, n);
            assertArrayEquals(new int[]{-1, 0, 7}, out);
        }
    }

    @Test
    void readBytesArrayBinaryOnly() {
        // Text/YAML use base64 and may compress; validate binary where exact bytes round‑trip is expected.
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        byte[] in = new byte[]{10, 20, 30};
        w.write("arr").sequence(v -> {
            v.uint8(in[0]);
            v.uint8(in[1]);
            v.uint8(in[2]);
        });

        byte[] out = new byte[3];
        int n = w.read("arr").array(out);
        assertEquals(3, n);
        assertArrayEquals(in, out);
    }
}
