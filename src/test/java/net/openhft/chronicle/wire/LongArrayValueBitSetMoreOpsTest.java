/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

public class LongArrayValueBitSetMoreOpsTest extends WireTestCommon {

    @Test
    public void previousAndRangeOps() {
        LongArrayValueBitSet bs = new LongArrayValueBitSet(256, new BinaryWire(Bytes.allocateElasticOnHeap(256)));
        try {
            bs.set(2, 70);
            assertTrue(bs.get(2));
            assertTrue(bs.get(69));
            assertEquals(69, bs.previousSetBit(127));

            bs.clear(10, 60);
            assertFalse(bs.get(10));
            assertEquals(60, bs.nextSetBit(10));
        } finally {
            bs.close();
        }
    }
}

