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

import static org.junit.Assert.*;

/**
 * Exercises ValueIn array readers using sequences for common wire types.
 */
public class ValueInArrayReadersTest extends WireTestCommon {

    @Test
    public void readDoubleArrayFromSequence() {
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
    public void readIntArrayFromSequence() {
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
    public void readBytesArrayBinaryOnly() {
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
