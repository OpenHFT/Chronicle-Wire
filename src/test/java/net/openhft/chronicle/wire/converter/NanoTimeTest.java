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

package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This test class extends the WireTestCommon, a class provided by the Chronicle Wire library
 * to facilitate the testing of wire-related functionality.
 */
public class NanoTimeTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * This test checks the serialization and deserialization functionality using
     * the NanoTime encoding format provided by the Chronicle Wire library.
     * The test uses a YAML-based wire to serialize data.
     */
    @Test
    public void yaml() {
        // Initialize a YAML wire on the heap.
        Wire wire = Wire.newYamlWireOnHeap();

        // Create a method writer instance for the UseNanoTime interface.
        UseNanoTime writer = wire.methodWriter(UseNanoTime.class);

        // Parse a date-time string to get its NanoTime representation.
        long ts = NanoTime.INSTANCE.parse("2022-06-17T12:35:56");

        // Use the writer to serialize the time and an event using the parsed NanoTime.
        writer.time(ts);
        writer.event(new Event(ts));

        // Verify the wire content matches the expected YAML format.
        assertEquals("" +
                "time: 2022-06-17T12:35:56\n" +
                "...\n" +
                "event: {\n" +
                "  start: 2022-06-17T12:35:56\n" +
                "}\n" +
                "...\n", wire.toString());
    }

    /**
     * An interface that declares two methods:
     * 1. time(): Takes a long representing NanoTime and writes it.
     * 2. event(): Takes an Event object and writes it.
     */
    interface UseNanoTime {
        void time(@NanoTime long time);

        void event(Event event);
    }

    /**
     * Event class represents an event with a start time.
     * It extends SelfDescribingMarshallable, a base class from the Chronicle Wire library
     * that automatically provides serialization and deserialization functionality for the object.
     */
    static class Event extends SelfDescribingMarshallable {
        @NanoTime
        private long start;

        Event(long start) {
            this.start = start;
        }
    }
}
