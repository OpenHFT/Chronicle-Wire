package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.DistributedUniqueTimeProvider;
import net.openhft.chronicle.bytes.MappedUniqueTimeProvider;
import net.openhft.chronicle.core.time.SystemTimeProvider;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

public class EgMain {
    static class Event extends SelfDescribingMarshallable {
        @LongConversion(NanoTimestampLongConverter.class)
        long time;
    }

    static long time;

    public static void main(String[] args) {
        DistributedUniqueTimeProvider tp = DistributedUniqueTimeProvider.forHostId(28);
        Event e = new Event();
        e.time = tp.currentTimeNanos();
/*
!net.openhft.chronicle.wire.EgMain$Event {
  time: 2021-12-28T14:07:02.954100128
}
*/
        String str = e.toString();
        Event e2 = Marshallable.fromString(str);
        System.out.println(e2);
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
