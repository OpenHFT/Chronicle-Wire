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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.DistributedUniqueTimeProvider;

// Main class illustrating event time serialization and performance measurement of time provision.
public class EgMain {

    // Nested class representing an event with a timestamp.
    static class Event extends SelfDescribingMarshallable {
        // Convert the long timestamp to nano-time format.
        @LongConversion(NanoTimestampLongConverter.class)
        long time;
    }

    static long time;

    public static void main(String[] args) {
        // Create a time provider for a specific host ID.
        DistributedUniqueTimeProvider tp = DistributedUniqueTimeProvider.forHostId(28);
        Event e = new Event();
        e.time = tp.currentTimeNanos();
        // Sample serialized format of the event.
/*
!net.openhft.chronicle.wire.EgMain$Event {
  time: 2021-12-28T14:07:02.954100128
}
*/
        String str = e.toString();
        Event e2 = Marshallable.fromString(str);
        System.out.println(e2);

        // Measure the time taken for retrieving current time repeatedly.
        for (int t = 0; t < 3; t++) {
            long start = System.nanoTime();
            int runs = 10000000;
            for (int i = 0; i < runs; i++)
                time = tp.currentTimeNanos();
            long delay = (System.nanoTime() - start) / runs;
            System.out.println(delay);
        }
    }
}
