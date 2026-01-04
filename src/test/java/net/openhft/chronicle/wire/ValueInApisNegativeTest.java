/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises less common ValueIn APIs, including empty sequences and binary bytesMatch behaviour.
 */
@SuppressWarnings({"deprecation", "removal"})
class ValueInApisNegativeTest extends WireTestCommon {

    @Test
    @DisplayName("Handles empty sequences and bytesMatch for binary wire")
    void emptySequenceAndBytesMatch() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            // empty sequence
            w.write("s").sequence(v -> {
            });
            int len = w.read("s").sequenceWithLength(new Object[0], (in, o) -> {
                int c = 0;
                while (in.hasNextSequenceItem()) {
                    in.skipValue();
                    c++;
                }
                return c;
            });
            assertEquals(0, len, "Expected empty sequence length for wireType=" + wt);

            // bytesMatch on binary only (text/yaml base64 specifics are covered elsewhere)
            if (wt == WireType.BINARY) {
                byte[] content = {9, 8, 7};
                w.write("b").bytes(content);
                final boolean[] res = {false};
                w.read("b").bytesMatch(Bytes.wrapForRead(content), b -> res[0] = b);
                assertTrue(res[0], "Expected bytesMatch to succeed for wireType=" + wt);
            }
        }
    }
}
