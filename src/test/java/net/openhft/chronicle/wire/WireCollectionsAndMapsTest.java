/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Exercises sequences and maps to hit ValueIn/ValueOut collection branches.
 */
public class WireCollectionsAndMapsTest extends WireTestCommon {

    @Test
    public void sequenceReadWrite() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            // empty and single element sequence
            w.write("empty").sequence(v -> {});
            w.write("one").sequence(v -> v.int32(7));
            // mixed types
            w.write("mix").sequence(v -> {
                v.text("a");
                v.int64(2L);
                v.float64(3.0);
            });

            // read back
            final int[] len = {0};
            len[0] = w.read("empty").sequenceWithLength(new Object[0], (in, arr) -> {
                int c = 0; while (in.hasNextSequenceItem()) { in.skipValue(); c++; } return c; });
            assertEquals(0, len[0]);

            len[0] = w.read("one").sequenceWithLength(new int[1], (in, arr) -> {
                int c = 0; while (in.hasNextSequenceItem()) { arr[c++] = in.int32(); } return c; });
            assertEquals(1, len[0]);

            Object[] out = new Object[3];
            w.read("mix").sequence(out, (arr, in) -> {
                arr[0] = in.text();
                arr[1] = in.int64();
                arr[2] = in.float64();
            });
            assertArrayEquals(new Object[]{"a", 2L, 3.0}, out);
        }
    }

    @Test
    public void mapsRoundTripViaMarshallable() {
        // Use ValueIn.marshallableAsMap across supported wire types.
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));
            Map<String, Object> in = new LinkedHashMap<>();
            in.put("k1", 1);
            in.put("k2", "v2");
            in.put("k3", 3L);
            w.write("m").map(in);

            Map<?, ?> out = w.read("m").marshallableAsMap(String.class, Object.class);
            assertNotNull(out);
            assertEquals(in, out);
        }
    }
}
