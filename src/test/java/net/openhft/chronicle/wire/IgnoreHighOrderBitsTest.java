/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

import java.io.DataOutput;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class IgnoreHighOrderBitsTest extends WireTestCommon {

    /**
     * Writes to the output stream the eight low-order bits of the argument b. The 24 high-order bits of b are ignored.
     * see https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/DataOutput.html#write(int)
     */
    @Test
    public void testWriteByte() throws IOException {
        // Create a new HexDumpBytes object to represent byte sequences in a human-readable format
        @SuppressWarnings("rawtypes") final Bytes<?> bytes = new HexDumpBytes();
        try {
            // Initialize a binary wire to serialize data to/from the bytes object
            final Wire wire = new BinaryWire(bytes);

            // Create a DataOutput object based on the binary wire to write data into it
            @SuppressWarnings("resource")
            DataOutput out = new WireObjectOutput(wire);

            // Integer value to test the writing process
            int b = 256;

            // Write the integer to the DataOutput object
            out.write(b);  // Only the low-order 8 bits should be written (expecting 0)

            // Assert that the byte representation matches the expected output
            assertEquals("" +
                            "a1 00                                           # 0\n",
                    bytes.toHexString());
        } finally {
            bytes.releaseLast();
        }
    }
}
