/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.DistributedUniqueTimeProvider;

// Main class illustrating event time serialization and performance measurement of time provision.
public class EgMain {

    public static void main(String[] args) {
        // Create a time provider for a specific host ID.
        DistributedUniqueTimeProvider tp = DistributedUniqueTimeProvider.forHostId(28);
        Event e = new Event();
        e.time = tp.currentTimeNanos();
        // Sample serialised format of the event.
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
            long time;
            for (int i = 0; i < runs; i++)
                time = tp.currentTimeNanos();
            long delay = (System.nanoTime() - start) / runs;
            System.out.println(delay);
        }
    }

    // Nested class representing an event with a timestamp.
    static class Event extends SelfDescribingMarshallable {
        // Convert the long timestamp to nano-time format.
        @LongConversion(NanoTimestampLongConverter.class)
        long time;
    }
}
