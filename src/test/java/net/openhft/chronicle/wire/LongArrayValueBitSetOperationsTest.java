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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Exercises {@link LongArrayValueBitSet} operations across multiple words.
 */
public class LongArrayValueBitSetOperationsTest extends WireTestCommon {

    @Test
    public void basicOps() {
        LongArrayValueBitSet bs = new LongArrayValueBitSet(192);
        bs.set(0);
        bs.set(64);
        bs.set(128);

        assertTrue(bs.get(0));
        assertTrue(bs.get(64));
        assertTrue(bs.get(128));

        assertEquals(0, bs.nextSetBit(0));
        assertEquals(64, bs.nextSetBit(1));
        assertEquals(128, bs.nextSetBit(65));

        bs.clear(64);
        assertFalse(bs.get(64));
        assertEquals(2, bs.cardinality());

        // Range operations
        bs.set(10, 15);
        for (int i = 10; i < 15; i++) assertTrue(bs.get(i));
        bs.clear(12, 14);
        assertTrue(bs.get(10));
        assertFalse(bs.get(12));
        assertTrue(bs.get(14));
    }
}

