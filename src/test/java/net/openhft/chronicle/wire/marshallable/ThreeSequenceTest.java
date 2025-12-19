/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Unit test for the ThreeSequence class.
 */
public class ThreeSequenceTest extends WireTestCommon {

    /**
     * Tests the serialization and deserialization process for the ThreeSequence class.
     */
    @Test
    public void testThree() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Deserialize the YAML string into a ThreeSequence object
        ThreeSequence ts = Marshallable.fromString("!" + ThreeSequence.class.getName() + " {\n" +
                "  a: [\n" +
                "    { price: 1.1, qty: 2.0 },\n" +
                "    { price: 1.2, qty: 1.0 }\n" +
                "  ]," +
                "  b: [\n" +
                "    { price: 2.1, qty: 2.0 },\n" +
                "    { price: 2.2, qty: 1.0 }\n" +
                "  ]," +
                "  c: [\n" +
                "    { price: 3.1, qty: 2.0 },\n" +
                "    { price: 3.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  text: hello\n" +
                "}\n");

        // Verify the toString() output of the ThreeSequence object
        assertEquals("!net.openhft.chronicle.wire.marshallable.ThreeSequence {\n" +
                "  a: [\n" +
                "    { price: 1.1, qty: 2.0 },\n" +
                "    { price: 1.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  b: [\n" +
                "    { price: 2.1, qty: 2.0 },\n" +
                "    { price: 2.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  c: [\n" +
                "    { price: 3.1, qty: 2.0 },\n" +
                "    { price: 3.2, qty: 1.0 }\n" +
                "  ],\n" +
                "  text: hello\n" +
                "}\n", ts.toString());

        // Round-trip test: serialize and then deserialize to verify the entire process
        assertEquals(ts, Marshallable.fromString(ts.toString()));
    }
}
