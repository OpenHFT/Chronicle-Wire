/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * see https://docs.oracle.com/javase/7/docs/api/java/io/DataOutput.html#write(int)
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
