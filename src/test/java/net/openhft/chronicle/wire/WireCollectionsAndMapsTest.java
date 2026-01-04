/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises sequences and maps to hit ValueIn/ValueOut collection branches.
 */
class WireCollectionsAndMapsTest extends WireTestCommon {

    @Test
    @DisplayName("Sequence read and write across wire types")
    void sequenceReadWrite() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            final Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            // empty and single element sequence
            w.write("empty").sequence(v -> {
            });
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
                int c = 0;
                while (in.hasNextSequenceItem()) {
                    in.skipValue();
                    c++;
                }
                return c;
            });
            assertEquals(0, len[0], "Empty sequence should report zero length for wire type " + wt);

            len[0] = w.read("one").sequenceWithLength(new int[1], (in, arr) -> {
                int c = 0;
                while (in.hasNextSequenceItem()) {
                    arr[c++] = in.int32();
                }
                return c;
            });
            assertEquals(1, len[0], "Single item sequence should report length one for wire type " + wt);

            Object[] out = new Object[3];
            w.read("mix").sequence(out, (arr, in) -> {
                arr[0] = in.text();
                arr[1] = in.int64();
                arr[2] = in.float64();
            });
            assertArrayEquals(new Object[]{"a", 2L, 3.0}, out,
                    "Mixed sequence should round trip values for wire type " + wt);
        }
    }

    @Test
    @DisplayName("Maps round trip via marshallable across wire types")
    void mapsRoundTripViaMarshallable() {
        // Use ValueIn.marshallableAsMap across supported wire types.
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Map<String, Object> in = new LinkedHashMap<>();
            in.put("k1", 1);
            in.put("k2", "v2");
            in.put("k3", 3L);

            final Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));
            w.write("m").map(in);

            Map<?, ?> out = w.read("m").marshallableAsMap(String.class, Object.class);
            assertNotNull(out, "Map should read back as non-null for wire type " + wt);
            assertEquals(in, out, "Map contents should round trip for wire type " + wt);
        }
    }
}
