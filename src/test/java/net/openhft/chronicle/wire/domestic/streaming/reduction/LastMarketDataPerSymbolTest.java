/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor.builder;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.replacingMerger;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.toConcurrentSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
class LastMarketDataPerSymbolTest extends WireTestCommon {

    private static final List<MarketData> MARKET_DATA_SET = Arrays.asList(
            new MarketData("MSFT", 100, 110, 90),
            new MarketData("AAPL", 200, 220, 180),
            new MarketData("MSFT", 101, 110, 90)
    );

    @Test
    @DisplayName("Reduces last market data per symbol")
    void lastMarketDataPerSymbol() {

        final Reduction<Map<String, MarketData>> listener = Reduction.of(
                        builder(MarketData.class).build())
                .collecting(collectingAndThen(toConcurrentMap(MarketData::symbol, Function.identity(), replacingMerger()), Collections::unmodifiableMap));

        MarketDataReductionTestSupport.acceptData(listener, MARKET_DATA_SET);

        final Map<String, MarketData> expected = MARKET_DATA_SET.stream()
                .collect(toMap(MarketData::symbol, Function.identity(), (a, b) -> b));

        assertEquals(expected, listener.reduction(),
                "Reduction should keep the last MarketData per symbol");
    }

    @Test
    @DisplayName("Reduces symbol set from market data")
    void symbolSet() {

        Reduction<Set<String>> listener = Reduction.of(
                        builder(MarketData.class).build().map(MarketData::symbol))
                .collecting(toConcurrentSet());

        MarketDataReductionTestSupport.acceptData(listener, MARKET_DATA_SET);

        final Set<String> expected = MARKET_DATA_SET.stream()
                .map(MarketData::symbol)
                .collect(toSet());

        assertEquals(expected, listener.reduction(),
                "Reduction should collect all unique symbols");
    }

}
