/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import net.openhft.chronicle.wire.domestic.reduction.Reductions;
import net.openhft.chronicle.wire.domestic.streaming.CreateUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collector;

import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.throwingMerger;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
class CountAccumulationTest extends WireTestCommon {

    @Test
    @DisplayName("Custom reduction should count three entries")
    void countCustom() {
        // Define a reduction that counts occurrences using a custom collector with AtomicLong
        Reduction<AtomicLong> listener = Reduction.of((wire, index) -> 1L)
                .collecting(Collector.of(AtomicLong::new, AtomicLong::addAndGet, throwingMerger(), Collector.Characteristics.CONCURRENT));

        // Execute the listener with test data
        count(listener);

        // Assert that the counted occurrences match the expected number (3)
        assertEquals(3, listener.reduction().get(),
                "Custom reduction should count three entries using AtomicLong collector");
    }

    @Test
    @DisplayName("Built-in reduction should count three entries")
    void countBuiltIn() {
        // Define a reduction that counts occurrences using a built-in counting method
        Reduction<LongSupplier> listener = Reductions.counting();

        // Execute the listener with test data
        count(listener);

        // Assert that the counted occurrences match the expected number (3)
        assertEquals(3, listener.reduction().getAsLong(),
                "Built-in reduction should count three entries using counting reduction");
    }

    // Helper method to simulate a test scenario, writing text to a wire and processing it with the provided listener
    private void count(Reduction<?> listener) {
        Wire wire = CreateUtil.create();

        wire.writeText("one");
        wire.writeText("two");
        wire.writeText("three");
        listener.accept(wire);
    }
}
