/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.LongSupplier;

import static net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor.extractingIndex;
import static net.openhft.chronicle.wire.domestic.reduction.Reductions.reducingLong;
import static net.openhft.chronicle.wire.domestic.streaming.CreateUtil.createThenValueOuts;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
class LastIndexSeenTest extends WireTestCommon {

    @Test
    @DisplayName("Reduction should report the last index seen")
    void lastIndexSeen() {
        Reduction<LongSupplier> listener = reducingLong(extractingIndex(), 0, (a, b) -> b);

        test(listener);

        long indexLastSeen = listener.reduction().getAsLong();
        assertEquals("16", Long.toHexString(indexLastSeen),
                "Last index should match the final document offset");
    }

    @Test
    @DisplayName("Reduction reports min and max index seen")
    void minAndMaxIndexSeen() {
        Reduction<LongSupplier> minListener = reducingLong(extractingIndex(), Long.MAX_VALUE, Math::min);
        Reduction<LongSupplier> maxListener = reducingLong(extractingIndex(), Long.MIN_VALUE, Math::max);

        test(minListener);
        test(maxListener);

        long min = minListener.reduction().getAsLong();
        long max = maxListener.reduction().getAsLong();

        assertEquals("4", Long.toHexString(min), "Min index should match first document offset");
        assertEquals("16", Long.toHexString(max), "Max index should match last document offset");
    }

    private void test(Reduction<?> listener) {
        // Add stuff that simulated existing values in the queue
        MarshallableIn tailer = createThenValueOuts(
                a -> a.writeString("one"),
                a -> a.writeString("two"),
                a -> a.writeString("three")
        );
        listener.accept(tailer);

    }
}
