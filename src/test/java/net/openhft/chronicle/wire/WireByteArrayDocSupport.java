/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WireByteArrayDocSupport {
    private WireByteArrayDocSupport() {
    }

    static void assertByteArrayDocuments(Wire wire, boolean usePadding) {
        wire.writeDocument(false, w -> w.write("nothing").object(new byte[0]));
        @NotNull byte[] one = {1};
        wire.writeDocument(false, w -> w.write("one").object(one));
        @NotNull byte[] four = {1, 2, 3, 4};
        wire.writeDocument(false, w -> w.write("four").object(four));

        final String expectedPadded = "--- !!data\n" +
                "nothing: !byte[] !!binary\n" +
                "# position: 32, header: 1\n" +
                "--- !!data\n" +
                "one: !byte[] !!binary AQ==\n" +
                "# position: 64, header: 2\n" +
                "--- !!data\n" +
                "four: !byte[] !!binary AQIDBA==\n";
        final String expectedNoPad = "--- !!data\n" +
                "nothing: !byte[] !!binary\n" +
                "# position: 30, header: 1\n" +
                "--- !!data\n" +
                "one: !byte[] !!binary AQ==\n" +
                "# position: 61, header: 2\n" +
                "--- !!data\n" +
                "four: !byte[] !!binary AQIDBA==\n";

        String expected = usePadding ? expectedPadded : expectedNoPad;
        String paddingLabel = usePadding ? "with padding" : "without padding";
        assertEquals(expected, Wires.fromSizePrefixedBlobs(wire.bytes(), 0),
                "Wire should render byte array documents " + paddingLabel);

        wire.readDocument(null, w -> assertArrayEquals(new byte[0],
                (byte[]) w.read(() -> "nothing").object(),
                "Empty byte array should round trip from nothing entry"));
        wire.readDocument(null, w -> assertArrayEquals(one,
                (byte[]) w.read(() -> "one").object(),
                "Single byte array should round trip from one entry"));
        wire.readDocument(null, w -> assertArrayEquals(four,
                (byte[]) w.read(() -> "four").object(),
                "Four byte array should round trip from four entry"));
    }
}
