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

public class LongValueBitSetMoreOpsTest extends WireTestCommon {

    private static LongValueBitSet bound(int bits) {
        return new LongValueBitSet(bits, new BinaryWire(Bytes.allocateElasticOnHeap(256)));
    }

    @Test
    public void previousAndNextClearBits() {
        LongValueBitSet bs = bound(256);
        try {
            bs.set(1); bs.set(63); bs.set(64); bs.set(200);
            assertEquals(0, bs.nextClearBit(0));
            assertEquals(62, bs.previousClearBit(63));
            assertEquals(65, bs.nextClearBit(65));
            assertEquals(199, bs.previousClearBit(200));
        } finally {
            bs.close();
        }
    }

    @Test
    public void streamEqualsCopyFromAndMarshallRoundTrip() {
        LongValueBitSet a = bound(128);
        LongValueBitSet b = bound(128);
        try {
            a.set(3); a.set(5); a.set(127);
            b.copyFrom(a);
            assertEquals(a, b);
            assertTrue(a.stream().anyMatch(i -> i == 3));

            Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));
            w.write("bs").object(a);
            LongValueBitSet r = w.read("bs").object(LongValueBitSet.class);
            assertEquals(a, r);
            r.close();
        } finally {
            a.close();
            b.close();
        }
    }
}

