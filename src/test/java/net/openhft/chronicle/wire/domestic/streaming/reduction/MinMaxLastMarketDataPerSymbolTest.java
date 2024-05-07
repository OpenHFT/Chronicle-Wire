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

import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import net.openhft.chronicle.wire.domestic.streaming.CreateUtil;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MinMaxLastMarketDataPerSymbolTest extends WireTestCommon {

    private static final List<MarketData> MARKET_DATA_SET = Arrays.asList(
            new MarketData("MSFT", 10, 11, 9),
            new MarketData("MSFT", 100, 110, 90),
            new MarketData("AAPL", 200, 220, 180)
    );

    @Test
    public void lastMarketDataPerSymbolCustom() {

        // This first Accumulation will keep track of the min and max value for all symbols

        final Reduction<MinMax> globalListener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).build())
                .collecting(
                        Collector.of(MinMax::new, MinMax::merge, throwingMerger(), Collector.Characteristics.CONCURRENT));

        // This second Accumulation will track min and max value for each symbol individually
        final Reduction<Map<String, MinMax>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).build())
                .collecting(
                        collectingAndThen(toConcurrentMap(MarketData::symbol, MinMax::new, MinMax::merge), Collections::unmodifiableMap)
                );

        test(globalListener);
        test(listener);

        final MinMax expectedGlobal = MARKET_DATA_SET.stream()
                .reduce(new MinMax(), MinMax::merge, MinMax::merge);

        final Map<String, MinMax> expected = MARKET_DATA_SET.stream()
                .collect(toMap(MarketData::symbol, MinMax::new, MinMax::merge));

        assertEquals(expectedGlobal, globalListener.reduction());
        assertEquals(expected, listener.reduction());
    }

    @Test
    public void lastMarketDataPerSymbol() {

        final Reduction<Map<String, MarketData>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).build())
                .collecting(
                        collectingAndThen(toConcurrentMap(MarketData::symbol, Function.identity(), replacingMerger()), Collections::unmodifiableMap)
                );

        test(listener);

        final Map<String, MarketData> expected = MARKET_DATA_SET.stream()
                .collect(toMap(MarketData::symbol, Function.identity(), replacingMerger()));

        assertEquals(expected, listener.reduction());
    }

    @Test
    public void symbolSet() {

        Reduction<Set<String>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class)
                                .withReusing(MarketData::new) // Reuse is safe as we only extract immutable data (String symbol).
                                .build()
                                .map(MarketData::symbol))
                .collecting(toConcurrentSet());

        test(listener);

        final Set<String> expected = MARKET_DATA_SET.stream()
                .map(MarketData::symbol)
                .collect(toSet());

        assertEquals(expected, listener.reduction());
    }

    private void test(Reduction<?> listener) {
        Wire wire = CreateUtil.create();
        MARKET_DATA_SET.forEach(md -> write(wire, md));
        listener.accept(wire);
    }

    private static void write(Wire appender, MarketData marketData) {
        try (final DocumentContext dc = appender.writingDocument()) {
            dc.wire().getValueOut().object(marketData);
        }
    }
}
