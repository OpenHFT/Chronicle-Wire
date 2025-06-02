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
