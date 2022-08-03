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

public class NanoTimeTest {
    @Test
    public void yaml() {
        Wire wire = Wire.newYamlWireOnHeap();
        UseNanoTime writer = wire.methodWriter(UseNanoTime.class);
        long ts = NanoTime.INSTANCE.parse("2022-06-17T12:35:56");
        writer.time(ts);
        writer.event(new Event(ts));
        assertEquals("" +
                "time: 2022-06-17T12:35:56\n" +
                "...\n" +
                "event: {\n" +
                "  start: 2022-06-17T12:35:56\n" +
                "}\n" +
                "...\n", wire.toString());
    }

    interface UseNanoTime {
        void time(@NanoTime long time);

        void event(Event event);
    }

    static class Event extends SelfDescribingMarshallable {
        @NanoTime
        private long start;

        Event(long start) {
            this.start = start;
        }
    }
}
