/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HexDumpTest extends WireTestCommon {

    // Test the endian behavior of HexDumpBytes
    @Test
    public void testEndian() {

        // If the native byte order isn't LITTLE_ENDIAN, we exit the test
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN)
            return;

        // Create a byte buffer with hex dump capabilities
        @SuppressWarnings("rawtypes")
        Bytes<?> b = new HexDumpBytes();

        // Write an integer value to the buffer
        b.writeInt(0x0a0b0c0d);

        // Assert that the byte representation is as expected
        assertEquals("0d 0c 0b 0a\n", b.toHexString());

        // Release the last byte buffer reference
        b.releaseLast();
    }
}
