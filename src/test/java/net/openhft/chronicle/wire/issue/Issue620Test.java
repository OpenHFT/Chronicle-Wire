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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import static org.junit.Assert.*;

public class Issue620Test extends net.openhft.chronicle.wire.WireTestCommon {
    @Test
    public void readBytes() {
        Wire wire = WireType.FIELDLESS_BINARY.apply(new HexDumpBytes());
        wire.writeDocument(w -> w.bytes().append("foo"));
        wire.writeDocument(w -> w.bytes().append("baz"));
        wire.writeDocument(w -> w.bytes().append("bar"));
        assertEquals("" +
                        "03 00 00 00 66 6f 6f                            # msg-length\n" +
                        "03 00 00 00 62 61 7a                            # msg-length\n" +
                        "03 00 00 00 62 61 72                            # msg-length\n",
                wire.bytes().toHexString());
        for (String s : "foo,baz,bar".split(",")) {
            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                assertEquals(s, dc.wire().bytes().toString());
            }
        }
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
        }
    }
}
