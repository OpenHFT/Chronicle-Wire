package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.internal.streaming.CreateUtil;
import net.openhft.chronicle.wire.internal.streaming.Reduction;
import net.openhft.chronicle.wire.internal.streaming.Reductions;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collector;

import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.throwingMerger;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CountAccumulationTest extends WireTestCommon {


    @Test
    public void countCustom() {
        Reduction<AtomicLong> listener = Reduction.of((wire, index) -> 1L)
                .collecting(Collector.of(AtomicLong::new, AtomicLong::addAndGet, throwingMerger(), Collector.Characteristics.CONCURRENT));

        count(listener);
        assertEquals(3, listener.reduction().get());
    }

    @Test
    public void countBuiltIn() {
        Reduction<LongSupplier> listener = Reductions.counting();
        count(listener);
        assertEquals(3, listener.reduction().getAsLong());
    }

    private void count(Reduction<?> listener) {
        Wire wire = CreateUtil.create();

        wire.writeText("one");
        wire.writeText("two");
        wire.writeText("three");
        listener.accept(wire);
    }
}