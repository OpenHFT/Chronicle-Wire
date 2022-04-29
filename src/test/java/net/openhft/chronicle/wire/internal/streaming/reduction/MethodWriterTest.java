package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.internal.streaming.CreateUtil;
import net.openhft.chronicle.wire.internal.streaming.DocumentExtractor;
import net.openhft.chronicle.wire.internal.streaming.Reduction;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toConcurrentMap;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.replacingMerger;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.throwingMerger;
import static org.junit.Assert.assertEquals;

public class MethodWriterTest extends WireTestCommon {


    private static final List<MarketData> MARKET_DATA_SET = Arrays.asList(
            new MarketData("MSFT", 10, 11, 9),
            new MarketData("MSFT", 100, 110, 90),
            new MarketData("AAPL", 200, 220, 180)
    );

    @Test
    public void lastSeen() {

        final Reduction<AtomicReference<MarketData>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class)
                                .withMethod(ServiceOut.class, ServiceOut::marketData).
                                build())
                .collecting(
                        Collector.of(AtomicReference<MarketData>::new, AtomicReference::set, throwingMerger(), Collector.Characteristics.CONCURRENT));

        test(listener);

        MarketData expected = createMarketData();
        MarketData actual = listener.reduction().get();
        assertEquals(expected, actual);
    }


    @Test
    public void map() {

        final Reduction<Map<String, MarketData>> listener = Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build())
                .collecting(collectingAndThen(toConcurrentMap(MarketData::symbol, Function.identity(), replacingMerger()), Collections::unmodifiableMap));

        test(listener);
        MarketData expectedSymbol = createMarketData();
        Map<String, MarketData> expected = new HashMap<>();
        expected.put(expectedSymbol.symbol(), expectedSymbol);

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