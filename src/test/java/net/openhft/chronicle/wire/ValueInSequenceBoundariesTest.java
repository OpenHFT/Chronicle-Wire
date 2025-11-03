/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Covers empty, single, and larger sequences to hit hasNextSequenceItem boundaries.
 */
public class ValueInSequenceBoundariesTest extends WireTestCommon {

    @Test
    public void emptyAndSingle() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));
            w.write("e").sequence(v -> {});
            w.write("s").sequence(v -> v.int64(1L));

            int n0 = w.read("e").sequenceWithLength(new Object[0], (in, o) -> {
                int c = 0; while (in.hasNextSequenceItem()) { in.skipValue(); c++; } return c; });
            assertEquals(0, n0);

            final long[] one = new long[1];
            int n1 = w.read("s").sequenceWithLength(one, (in, arr) -> {
                int c = 0; while (in.hasNextSequenceItem()) { arr[c++] = in.int64(); } return c; });
            assertEquals(1, n1);
            assertEquals(1L, one[0]);
        }
    }

    @Test
    public void manyItems() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(1024));
            w.write("m").sequence(v -> {
                for (int i = 0; i < 65; i++) v.int32(i);
            });
            int[] arr = new int[65];
            int n = w.read("m").array(arr);
            assertEquals(65, n);
            for (int i = 0; i < 65; i++) assertEquals(i, arr[i]);
        }
    }
}

