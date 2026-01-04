/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("all")
class DocumentContextTest extends WireTestCommon {

    // Test multi-message serialization in TEXT format.
    @Test
    void multiMessageText() {
        // Create a wire of TEXT type
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        // Serialize messages and retrieve bytes
        Bytes<?> bytes = doTest(wire);
        bytes.readSkip(4);

        // Check serialization format
        assertEquals("one: 1\n" +
                "two: 2\n" +
                "three: 3\n", bytes.toString());
        bytes.releaseLast();
    }

    // Test multi-message serialization in BINARY format.
    @Test
    @SuppressWarnings("deprecation")
    void multiMessageBinary() {
        // Create a wire of BINARY type with hex dump
        BinaryWire wire = new BinaryWire(new HexDumpBytes());
        wire.usePadding(true);

        // Serialize messages and retrieve bytes
        Bytes<?> bytes = doTest(wire);

        // Check serialization format
        assertEquals("" +
                        "17 00 00 00                                     # msg-length\n" +
                        "b9 03 6f 6e 65                                  # one: (event)\n" +
                        "a1 01                                           # 1\n" +
                        "b9 03 74 77 6f                                  # two: (event)\n" +
                        "a1 02                                           # 2\n" +
                        "b9 05 74 68 72 65 65                            # three: (event)\n" +
                        "a1 03                                           # 3\n",
                bytes.toHexString());
        bytes.releaseLast();
    }

    // Helper method to create serialized messages.
    @NotNull
    private Bytes<?> doTest(Wire wire) {
        wire.acquireWritingDocument(false).wire().writeEventName("one").int16(1);
        wire.acquireWritingDocument(false).wire().writeEventName("two").int16(2);
        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            dc.wire().writeEventName("three").int16(3);
            close(dc);
            close(dc);
        }
        return wire.bytes();
    }

    private static void close(DocumentContext dc) {
        dc.close();
    }
}
