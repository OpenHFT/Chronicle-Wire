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

public class DocumentContextTest extends WireTestCommon {
    @Test
    public void multiMessageText() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        Bytes<?> bytes = doTest(wire);
        bytes.readSkip(4);
        assertEquals("one: 1\n" +
                "two: 2\n" +
                "three: 3\n", bytes.toString());
        bytes.releaseLast();
    }

    @Test
    public void multiMessageBinary() {
        BinaryWire wire = new BinaryWire(new HexDumpBytes());
        wire.usePadding(true);
        Bytes<?> bytes = doTest(wire);
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

    @NotNull
    private Bytes<?> doTest(Wire wire) {
        wire.acquireWritingDocument(false).wire().writeEventName("one").int16(1);
        wire.acquireWritingDocument(false).wire().writeEventName("two").int16(2);
        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            dc.wire().writeEventName("three").int16(3);
            dc.close();
            dc.close();
        }
        return wire.bytes();
    }
}
