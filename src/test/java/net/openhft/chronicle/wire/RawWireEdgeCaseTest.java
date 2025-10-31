/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import static org.junit.Assert.*;

public class RawWireEdgeCaseTest extends WireTestCommon {

    @Test
    public void writesAndReadsPrimitives() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        RawWire wire = new RawWire(bytes);

        wire.write().int32(10);
        wire.write().text("hello");
        wire.write().int64(20L);

        bytes.readPositionRemaining(0, bytes.writePosition());
        assertEquals(10, wire.read().int32());
        assertEquals("hello", wire.read().text());
        assertEquals(20L, wire.read().int64());

        // reading past end should return default values (0 or null)
        wire.read().int32();
    }
}

