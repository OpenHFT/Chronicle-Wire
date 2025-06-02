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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireParserTest extends WireTestCommon {

    @Test
    public void noOpReadOne() {
        // Create an elastic byte buffer with an initial capacity of 128 bytes.
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        // Write a sample string "Hello world" to the buffer.
        wire.bytes().writeUtf8("Hello world");

        // Skip all readable bytes in the wire.
        WireParser.skipReadable(-1, wire);

        // Assert that there are no more bytes left to read from the wire.
        assertEquals(0, wire.bytes().readRemaining());
    }
}
