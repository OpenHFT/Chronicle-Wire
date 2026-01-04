/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WireParserTest extends WireTestCommon {

    @Test
    @DisplayName("SkipReadable consumes all remaining readable bytes")
    public void noOpReadOne() {
        // Create an elastic byte buffer with an initial capacity of 128 bytes.
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        // Write a sample string "Hello world" to the buffer.
        wire.bytes().writeUtf8("Hello world");

        // Skip all readable bytes in the wire.
        WireParser.skipReadable(-1, wire);

        // Assert that there are no more bytes left to read from the wire.
        assertEquals(0,
                wire.bytes().readRemaining(),
                "SkipReadable should consume all readable bytes");
    }
}
