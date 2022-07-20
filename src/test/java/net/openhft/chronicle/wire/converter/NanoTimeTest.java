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
