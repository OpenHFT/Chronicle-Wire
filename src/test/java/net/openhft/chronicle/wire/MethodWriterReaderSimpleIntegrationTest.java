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
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Small end‑to‑end flow using MethodWriter and VanillaMethodReader to
 * exercise method dispatch and basic argument serialisation.
 */
public class MethodWriterReaderSimpleIntegrationTest extends WireTestCommon {

    interface Echo {
        void one(int v);

        void two(String t);

        void three(long a, double b);
    }

    @Test
    public void roundTrip() {
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));

        Echo writer = w.methodWriter(Echo.class);
        writer.one(7);
        writer.two("hi");
        writer.three(11L, Math.E);

        List<String> seen = new ArrayList<>();
        MethodReader reader = w.methodReader(new Echo() {
            @Override
            public void one(int v) { seen.add("one:" + v); }
            @Override
            public void two(String t) { seen.add("two:" + t); }
            @Override
            public void three(long a, double b) { seen.add("three:" + a + "," + b); }
        });

        while (reader.readOne()) {
            // loop until exhausted
        }

        assertEquals(3, seen.size());
        assertTrue(seen.get(0).startsWith("one:"));
        assertTrue(seen.get(1).startsWith("two:"));
        assertTrue(seen.get(2).startsWith("three:"));
    }
}
