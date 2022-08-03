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
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import org.junit.Test;

import java.util.function.LongSupplier;

import static net.openhft.chronicle.wire.domestic.streaming.CreateUtil.createThenValueOuts;
import static net.openhft.chronicle.wire.domestic.reduction.Reductions.reducingLong;
import static net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor.extractingIndex;
import static org.junit.Assert.assertEquals;

public class LastIndexSeenTest extends WireTestCommon {

    @Test
    public void lastIndexSeen() {
        Reduction<LongSupplier> listener = reducingLong(extractingIndex(), 0, (a, b) -> b);

        test(listener);

        long indexLastSeen = listener.reduction().getAsLong();
        assertEquals("16", Long.toHexString(indexLastSeen));
    }

    @Test
    public void minAndMaxIndexSeen() {
        Reduction<LongSupplier> minListener = reducingLong(extractingIndex(), Long.MAX_VALUE, Math::min);
        Reduction<LongSupplier> maxListener = reducingLong(extractingIndex(), Long.MIN_VALUE, Math::max);

        test(minListener);
        test(maxListener);

        long min = minListener.reduction().getAsLong();
        long max = maxListener.reduction().getAsLong();

        assertEquals("4", Long.toHexString(min));
        assertEquals("16", Long.toHexString(max));
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