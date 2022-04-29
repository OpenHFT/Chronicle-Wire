package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.internal.streaming.CreateUtil;
import net.openhft.chronicle.wire.internal.streaming.DocumentExtractor;
import net.openhft.chronicle.wire.internal.streaming.Reduction;
import net.openhft.chronicle.wire.internal.streaming.Reductions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.reducingConcurrent;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.replacingMerger;
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

        Reduction<MarketData> listener = Reductions.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build())
                .collecting(lastSeen);

        test(listener);

        MarketData expected = createMarketData();
        MarketData actual = listener.reduction();
        assertEquals(expected, actual);
    }

    @Test
    public void lastSeen() {

        Reduction<Optional<MarketData>> listener = Reductions.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build())
                .collecting(reducingConcurrent(replacingMerger()));

        test(listener);

        MarketData expected = createMarketData();
        MarketData actual = listener.reduction().orElseThrow(NoSuchElementException::new);
        assertEquals(expected, actual);
    }

    @Test
    public void map() {

        Reduction<Map<String, MarketData>> listener = Reductions.of(
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

        final Reduction<Map<String, List<Double>>> listener = Reductions.of(
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