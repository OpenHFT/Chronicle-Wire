//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Drives less common ValueIn APIs and empty sequence paths.
 */
public class ValueInApisNegativeTest extends WireTestCommon {

    @Test
    public void emptySequenceAndBytesMatch() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            // empty sequence
            w.write("s").sequence(v -> {});
            int len = w.read("s").sequenceWithLength(new Object[0], (in, o) -> {
                int c = 0; while (in.hasNextSequenceItem()) { in.skipValue(); c++; } return c; });
            assertEquals(0, len);

            // bytesMatch on binary only (text/yaml base64 specifics are covered elsewhere)
            if (wt == WireType.BINARY) {
                byte[] content = new byte[]{9, 8, 7};
                w.write("b").bytes(content);
                final boolean[] res = {false};
                w.read("b").bytesMatch(Bytes.wrapForRead(content), b -> res[0] = b);
                assertTrue(res[0]);
            }
        }
    }
}
