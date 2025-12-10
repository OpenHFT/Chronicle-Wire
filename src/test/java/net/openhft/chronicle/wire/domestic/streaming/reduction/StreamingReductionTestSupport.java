package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import net.openhft.chronicle.wire.domestic.streaming.CreateUtil;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toConcurrentMap;
import static net.openhft.chronicle.wire.domestic.reduction.ConcurrentCollectors.replacingMerger;

@SuppressWarnings("deprecation")
final class StreamingReductionTestSupport {
    private StreamingReductionTestSupport() {
    }

    static Reduction<Map<String, MarketData>> mapReduction() {
        return Reduction.of(
                        DocumentExtractor.builder(MarketData.class).withMethod(ServiceOut.class, ServiceOut::marketData).build())
                .collecting(collectingAndThen(toConcurrentMap(MarketData::symbol, Function.identity(), replacingMerger()), Collections::unmodifiableMap));
    }

    static MarketData createMarketData() {
        return new MarketData("MSFT", 100, 110, 90);
    }

    static void playMarketData(Reduction<?> listener) {
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

    interface ServiceOut {

        void marketData(MarketData marketData);

        void greeting(String greeting);
    }
}
