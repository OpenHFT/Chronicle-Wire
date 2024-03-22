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
        final Bytes<?> bytes = new HexDumpBytes();
        try {
            final Wire wire = new BinaryWire(bytes);
            @SuppressWarnings("resource")
            DataOutput out = new WireObjectOutput(wire);
            int b = 256;
            out.write(b); // expecting 0 to be written

            assertEquals("" +
                            "a1 00                                           # 0\n",
                    bytes.toHexString());
        } finally {
            bytes.releaseLast();
        }
    }
}
