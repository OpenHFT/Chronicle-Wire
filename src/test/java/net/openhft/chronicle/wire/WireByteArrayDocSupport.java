package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        assertEquals(expected, Wires.fromSizePrefixedBlobs(wire.bytes(), 0));

        wire.readDocument(null, w -> assertArrayEquals(new byte[0], (byte[]) w.read(() -> "nothing").object()));
        wire.readDocument(null, w -> assertArrayEquals(one, (byte[]) w.read(() -> "one").object()));
        wire.readDocument(null, w -> assertArrayEquals(four, (byte[]) w.read(() -> "four").object()));
    }
}
