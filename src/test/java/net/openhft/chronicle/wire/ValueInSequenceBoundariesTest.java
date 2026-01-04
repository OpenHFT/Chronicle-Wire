/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers empty, single, and larger sequences to hit hasNextSequenceItem boundary edge scenarios.
 */
@SuppressWarnings({"deprecation", "removal"})
class ValueInSequenceBoundariesTest extends WireTestCommon {

    @Test
    @DisplayName("Handles empty and single sequence boundaries")
    void emptyAndSingle() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));
            w.write("e").sequence(v -> {
                // empty sequence
            });
            w.write("s").sequence(v -> v.int64(1L));

            int n0 = w.read("e").sequenceWithLength(new Object[0], (in, o) -> {
                int c = 0;
                while (in.hasNextSequenceItem()) {
                    in.skipValue();
                    c++;
                }
                return c;
            });
            assertEquals(0, n0, "Empty sequence length should be 0 for wireType=" + wt);

            final long[] one = new long[1];
            int n1 = w.read("s").sequenceWithLength(one, (in, arr) -> {
                int c = 0;
                while (in.hasNextSequenceItem()) {
                    arr[c++] = in.int64();
                }
                return c;
            });
            assertEquals(1, n1, "Single-item sequence length should be 1 for wireType=" + wt);
            assertEquals(1L, one[0], "Single item value should be 1 for wireType=" + wt);
        }
    }

    @Test
    @DisplayName("Handles multiple sequence items across boundary")
    void manyItems() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(1024));
            w.write("m").sequence(v -> {
                for (int i = 0; i < 65; i++) {
                    v.int32(i);
                }
            });
            int[] arr = new int[65];
            int n = w.read("m").array(arr);
            assertEquals(65, n, "Sequence length should be 65 for wireType=" + wt);
            for (int i = 0; i < 65; i++) {
                assertEquals(i, arr[i], "Sequence value should match for wireType=" + wt + ", index=" + i);
            }
        }
    }
}
