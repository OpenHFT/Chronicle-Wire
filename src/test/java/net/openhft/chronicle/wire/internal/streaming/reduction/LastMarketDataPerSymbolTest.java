package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.internal.streaming.CreateUtil;
import net.openhft.chronicle.wire.internal.streaming.Reduction;
import net.openhft.chronicle.wire.internal.streaming.Reductions;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.replacingMerger;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.toConcurrentSet;
import static net.openhft.chronicle.wire.internal.streaming.DocumentExtractor.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LastMarketDataPerSymbolTest extends WireTestCommon {


    private static final List<MarketData> MARKET_DATA_SET = Arrays.asList(
            new MarketData("MSFT", 100, 110, 90),
            new MarketData("AAPL", 200, 220, 180),
            new MarketData("MSFT", 101, 110, 90)
    );

    @Test
    public void lastMarketDataPerSymbol() {

        final Reduction<Map<String, MarketData>> listener = Reductions.of(
                        builder(MarketData.class).build())
                .collecting(collectingAndThen(toConcurrentMap(MarketData::symbol, Function.identity(), replacingMerger()), Collections::unmodifiableMap));

        test(listener);

        final Map<String, MarketData> expected = MARKET_DATA_SET.stream()
                .collect(toMap(MarketData::symbol, Function.identity(), (a, b) -> b));

        assertEquals(expected, listener.reduction());
    }

    @Test
    public void symbolSet() {

        Reduction<Set<String>> listener = Reductions.of(
                        builder(MarketData.class).build().map(MarketData::symbol))
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