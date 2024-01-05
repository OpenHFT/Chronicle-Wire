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

package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import net.openhft.chronicle.wire.domestic.reduction.Reductions;
import net.openhft.chronicle.wire.domestic.streaming.CreateUtil;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collector;

import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.throwingMerger;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CountAccumulationTest extends WireTestCommon {

    @Test
    public void countCustom() {
        // Define a reduction that counts occurrences using a custom collector with AtomicLong
        Reduction<AtomicLong> listener = Reduction.of((wire, index) -> 1L)
                .collecting(Collector.of(AtomicLong::new, AtomicLong::addAndGet, throwingMerger(), Collector.Characteristics.CONCURRENT));

        // Execute the listener with test data
        count(listener);

        // Assert that the counted occurrences match the expected number (3)
        assertEquals(3, listener.reduction().get());
    }

    @Test
    public void countBuiltIn() {
        // Define a reduction that counts occurrences using a built-in counting method
        Reduction<LongSupplier> listener = Reductions.counting();

        // Execute the listener with test data
        count(listener);

        // Assert that the counted occurrences match the expected number (3)
        assertEquals(3, listener.reduction().getAsLong());
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
