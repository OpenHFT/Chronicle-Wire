/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;

import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.throwingMerger;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
class MethodWriterTest extends WireTestCommon {

    @Test
    @DisplayName("Reduction captures last market data via method writer")
    void lastSeen() {

        final Reduction<AtomicReference<MarketData>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class)
                                .withMethod(StreamingReductionTestSupport.ServiceOut.class, StreamingReductionTestSupport.ServiceOut::marketData).
                                build())
                .collecting(
                        Collector.of(AtomicReference<MarketData>::new, AtomicReference::set, throwingMerger(), Collector.Characteristics.CONCURRENT));

        StreamingReductionTestSupport.playMarketData(listener);

        MarketData expected = StreamingReductionTestSupport.createMarketData();
        MarketData actual = listener.reduction().get();
        assertEquals(expected, actual, "Reduction should capture the last MarketData");
    }

    @Test
    @DisplayName("Reduction map collects market data by symbol")
    void map() {

        final Reduction<Map<String, MarketData>> listener = StreamingReductionTestSupport.mapReduction();

        StreamingReductionTestSupport.playMarketData(listener);
        MarketData expectedSymbol = StreamingReductionTestSupport.createMarketData();
        Map<String, MarketData> expected = new HashMap<>();
        expected.put(expectedSymbol.symbol(), expectedSymbol);

        assertEquals(expected, listener.reduction(),
                "Reduction should map the last MarketData per symbol");
    }
}
