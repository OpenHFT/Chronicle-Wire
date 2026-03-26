/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Small end‑to‑end flow using MethodWriter and VanillaMethodReader to
 * exercise method dispatch and basic argument serialisation.
 */
class MethodWriterReaderSimpleIntegrationTest extends WireTestCommon {

    interface Echo {
        void one(int v);

        void two(String t);

        void three(long a, double b);
    }

    @Test
    void roundTrip() {
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap(256));

        Echo writer = w.methodWriter(Echo.class);
        writer.one(7);
        writer.two("hi");
        writer.three(11L, Math.E);

        List<String> seen = new ArrayList<>();
        MethodReader reader = w.methodReader(new Echo() {
            @Override
            public void one(int v) {
                seen.add("one:" + v);
            }

            @Override
            public void two(String t) {
                seen.add("two:" + t);
            }

            @Override
            public void three(long a, double b) {
                seen.add("three:" + a + "," + b);
            }
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
