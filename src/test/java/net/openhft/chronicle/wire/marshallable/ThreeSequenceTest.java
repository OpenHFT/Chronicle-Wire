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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for the ThreeSequence class.
 */
public class ThreeSequenceTest extends WireTestCommon {

    /**
     * Tests the serialization and deserialization process for the ThreeSequence class.
     */
    @Test
    public void testThree() {
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
