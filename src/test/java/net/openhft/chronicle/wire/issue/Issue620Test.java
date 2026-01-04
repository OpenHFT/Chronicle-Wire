/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class extending `WireTestCommon` to validate the reading and writing of bytes using FIELDLESS_BINARY wire type.
 */
class Issue620Test extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Test method that writes three strings "foo", "baz", and "bar" to a FIELDLESS_BINARY wire and then reads them back.
     * Also asserts that the bytes written to the wire match the expected bytes and that they can be read back correctly.
     */
    @Test
    @DisplayName("FIELDLESS_BINARY preserves raw bytes per document")
    void readBytes() {
        // Creates a FIELDLESS_BINARY wire with a backing store of HexDumpBytes
        Wire wire = WireType.FIELDLESS_BINARY.apply(new HexDumpBytes());

        // Writes three strings as bytes to the wire
        wire.writeDocument(w -> w.bytes().append("foo"));
        wire.writeDocument(w -> w.bytes().append("baz"));
        wire.writeDocument(w -> w.bytes().append("bar"));

        // Asserts that the bytes in the wire match the expected representation
        assertEquals("03 00 00 00 66 6f 6f                            # msg-length\n" +
                        "03 00 00 00 62 61 7a                            # msg-length\n" +
                        "03 00 00 00 62 61 72                            # msg-length\n",
                wire.bytes().toHexString(),
                "Wire bytes should match expected fieldless payloads");

        // Reads the bytes back from the wire and asserts they match the strings "foo", "baz", and "bar"
        for (String s : "foo,baz,bar".split(",")) {
            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent(), "Document should be present for " + s);
                assertEquals(s, dc.wire().bytes().toString(),
                        "Document payload should match " + s);
            }
        }

        // Attempts to read one more document from the wire and asserts that it is not present
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(), "Wire should not contain additional documents");
        }
    }
}
