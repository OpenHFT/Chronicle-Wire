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

import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import net.openhft.chronicle.wire.domestic.streaming.CreateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.reducingConcurrent;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.replacingMerger;
import static org.junit.Assert.assertEquals;

public class CollectorTest extends WireTestCommon {

    private static final String Q_NAME = CollectorTest.class.getSimpleName();

    private static final List<MarketData> MARKET_DATA_SET = Arrays.asList(
            new MarketData("MSFT", 10, 11, 9),
            new MarketData("MSFT", 100, 110, 90),
            new MarketData("AAPL", 200, 220, 180)
    );

    @Before
    public void clearBefore() {
        IOTools.deleteDirWithFiles(Q_NAME);
    }

    @After
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
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build())
                .collecting(lastSeen);

        test(listener);

        MarketData expected = createMarketData();
        MarketData actual = listener.reduction();
        assertEquals(expected, actual);
    }

    @Test
    public void lastSeen() {

        Reduction<Optional<MarketData>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build())
                .collecting(reducingConcurrent(replacingMerger()));

        test(listener);

        MarketData expected = createMarketData();
        MarketData actual = listener.reduction().orElseThrow(NoSuchElementException::new);
        assertEquals(expected, actual);
    }

    @Test
    public void map() {

        Reduction<Map<String, MarketData>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build()
                )
                .collecting(collectingAndThen(toConcurrentMap(MarketData::symbol, Function.identity(), replacingMerger()), Collections::unmodifiableMap));

        test(listener);

        MarketData expectedSymbol = createMarketData();
        Map<String, MarketData> expected = new HashMap<>();
        expected.put(expectedSymbol.symbol(), expectedSymbol);

        assertEquals(expected, listener.reduction());
        assertEquals("java.util.Collections$UnmodifiableMap", listener.reduction().getClass().getName());
    }

    @Test
    public void composite() {

        final Reduction<Map<String, List<Double>>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build())
                .collecting(groupingByConcurrent(MarketData::symbol, mapping(MarketData::last, toList())));

        test(listener);
        MarketData expectedSymbol = createMarketData();

        Map<String, List<Double>> expected = new HashMap<>();
        expected.put(expectedSymbol.symbol(), Arrays.asList(0D, expectedSymbol.last()));

        assertEquals(expected, listener.reduction());
    }

    private void test(Reduction<?> listener) {
        Wire wire = CreateUtil.create();
        ServiceOut serviceOut = wire.methodWriter(ServiceOut.class);

        MarketData marketData = createMarketData();
        marketData.last(0);

        serviceOut.marketData(marketData);
        serviceOut.greeting("Bonjour");
        serviceOut.marketData(createMarketData());
        serviceOut.greeting("Guten Tag");
        listener.accept(wire);
    }

    static MarketData createMarketData() {
        return new MarketData("MSFT", 100, 110, 90);
    }

    public interface ServiceOut {

        void marketData(MarketData marketData);

        void greeting(String greeting);
    }

}
