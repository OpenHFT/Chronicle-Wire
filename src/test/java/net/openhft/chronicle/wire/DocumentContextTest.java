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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("all")
public class DocumentContextTest extends WireTestCommon {

    // Test multi-message serialization in TEXT format.
    @Test
    public void multiMessageText() {
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
    @SuppressWarnings("deprecation")
    @Test
    public void multiMessageBinary() {
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
