//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import net.openhft.chronicle.wire.domestic.reduction.Reductions;
import org.junit.Test;

import java.util.function.LongSupplier;

import static net.openhft.chronicle.wire.domestic.streaming.CreateUtil.createThenValueOuts;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LastIndexSeenTailerTest extends WireTestCommon {

    @Test
    public void lastIndexSeenTailer() {

        // Add stuff that simulated existing values in the queue
        MarshallableIn tailer = createThenValueOuts(
                a -> a.writeString("one"),
                a -> a.writeString("two"),
                a -> a.writeString("three")
        );

        // Define a reduction to obtain the last seen index from a tailer
        final Reduction<LongSupplier> listener = Reductions.reducingLong(ToLongDocumentExtractor.extractingIndex(), 0, (a, b) -> b);

        // Process the tailer with the defined listener
        listener.accept(tailer);

        // Assert that the retrieved index is greater than the sum of the lengths of the simulated strings (3 + 3 + 5)
        assertTrue(listener.reduction().getAsLong() > 3 + 3 + 5);
    }
}
