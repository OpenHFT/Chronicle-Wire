/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.reducingConcurrent;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.replacingMerger;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
public class CollectorTest extends WireTestCommon {

    private static final String Q_NAME = CollectorTest.class.getSimpleName();

    @BeforeEach
    public void clearBefore() {
        IOTools.deleteDirWithFiles(Q_NAME);
    }

    @AfterEach
    public void clearAfter() {
        IOTools.deleteDirWithFiles(Q_NAME);
    }

    @Test
    public void lastSeenManual() {

        Collector<MarketData, AtomicReference<MarketData>, MarketData> lastSeen = Collector.of(
                AtomicReference::new,
                AtomicReference::set,
                (a, b) -> a,
                AtomicReference::get,
                Collector.Characteristics.CONCURRENT
        );

        Reduction<MarketData> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(StreamingReductionTestSupport.ServiceOut.class, StreamingReductionTestSupport.ServiceOut::marketData).build())
                .collecting(lastSeen);

        StreamingReductionTestSupport.playMarketData(listener);

        MarketData expected = StreamingReductionTestSupport.createMarketData();
        MarketData actual = listener.reduction();
        assertEquals(expected, actual);
    }

    @Test
    public void lastSeen() {
        Reduction<Optional<MarketData>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(StreamingReductionTestSupport.ServiceOut.class, StreamingReductionTestSupport.ServiceOut::marketData).build())
                .collecting(reducingConcurrent(replacingMerger()));

        StreamingReductionTestSupport.playMarketData(listener);

        MarketData expected = StreamingReductionTestSupport.createMarketData();
        MarketData actual = listener.reduction().orElseThrow(NoSuchElementException::new);
        assertEquals(expected, actual);
    }

    @Test
    public void map() {

        Reduction<Map<String, MarketData>> listener = StreamingReductionTestSupport.mapReduction();

        StreamingReductionTestSupport.playMarketData(listener);

        MarketData expectedSymbol = StreamingReductionTestSupport.createMarketData();
        Map<String, MarketData> expected = new HashMap<>();
        expected.put(expectedSymbol.symbol(), expectedSymbol);

        assertEquals(expected, listener.reduction());
        assertEquals("java.util.Collections$UnmodifiableMap", listener.reduction().getClass().getName());
    }

    @Test
    public void composite() {

        final Reduction<Map<String, List<Double>>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(StreamingReductionTestSupport.ServiceOut.class, StreamingReductionTestSupport.ServiceOut::marketData).build())
                .collecting(groupingByConcurrent(MarketData::symbol, mapping(MarketData::last, toList())));

        StreamingReductionTestSupport.playMarketData(listener);
        MarketData expectedSymbol = StreamingReductionTestSupport.createMarketData();

        Map<String, List<Double>> expected = new HashMap<>();
        expected.put(expectedSymbol.symbol(), Arrays.asList(0D, expectedSymbol.last()));

        assertEquals(expected, listener.reduction());
    }
}
