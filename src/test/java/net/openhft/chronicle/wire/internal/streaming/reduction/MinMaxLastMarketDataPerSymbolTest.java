package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.internal.streaming.CreateUtil;
import net.openhft.chronicle.wire.internal.streaming.DocumentExtractor;
import net.openhft.chronicle.wire.internal.streaming.Reduction;
import net.openhft.chronicle.wire.internal.streaming.Reductions;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.*;
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

        final Reduction<MinMax> globalListener = Reductions.of(
                        DocumentExtractor.builder(MarketData.class).build())
                .collecting(
                        Collector.of(MinMax::new, MinMax::merge, throwingMerger(), Collector.Characteristics.CONCURRENT));

        // This second Accumulation will track min and max value for each symbol individually
        final Reduction<Map<String, MinMax>> listener = Reductions.of(
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

        final Reduction<Map<String, MarketData>> listener = Reductions.of(
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

        Reduction<Set<String>> listener = Reductions.of(
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