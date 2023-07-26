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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericMethodWriterTest extends net.openhft.chronicle.wire.WireTestCommon {
    @Test
    public void genericParameter() {
        Wire wire = Wire.newYamlWireOnHeap();
        final ChronicleEventHandler writer = wire.methodWriter(ChronicleEventHandler.class);
        final ChronicleEvent event = new ChronicleEvent();
        event.sendingTimeNS((long) 1e9);
        writer.event(event);
        event.sendingTimeNS((long) 2e9);
        writer.onEvent(event);

        assertEquals("" +
                "event: {\n" +
                "  sendingTimeNS: 1970-01-01T00:00:01,\n" +
                "  transactTimeNS: 0,\n" +
                "  text1: \"\",\n" +
                "  text3: !!null \"\"\n" +
                "}\n" +
                "...\n" +
                "onEvent: {\n" +
                "  sendingTimeNS: 1970-01-01T00:00:02,\n" +
                "  transactTimeNS: 0,\n" +
                "  text1: \"\",\n" +
                "  text3: !!null \"\"\n" +
                "}\n" +
                "...\n", wire.toString());

        Wire wire2 = Wire.newYamlWireOnHeap();
        final ChronicleEventHandler writer2 = wire2.methodWriter(ChronicleEventHandler.class);
        final MethodReader reader = wire.methodReader(writer2);
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());

        assertEquals("" +
                "event: {\n" +
                "  sendingTimeNS: 1970-01-01T00:00:01,\n" +
                "  transactTimeNS: 0,\n" +
                "  text1: \"\",\n" +
                "  text3: !!null \"\"\n" +
                "}\n" +
                "...\n" +
                "onEvent: {\n" +
                "  sendingTimeNS: 1970-01-01T00:00:02,\n" +
                "  transactTimeNS: 0,\n" +
                "  text1: \"\",\n" +
                "  text3: !!null \"\"\n" +
                "}\n" +
                "...\n", wire2.toString());
    }
}
