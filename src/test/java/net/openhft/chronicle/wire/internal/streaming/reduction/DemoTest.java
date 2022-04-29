package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.internal.streaming.*;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingByConcurrent;
import static java.util.stream.Collectors.summarizingDouble;
import static net.openhft.chronicle.wire.internal.streaming.ConcurrentCollectors.replacingMerger;
import static net.openhft.chronicle.wire.internal.streaming.DocumentExtractor.builder;
import static net.openhft.chronicle.wire.internal.streaming.ToLongDocumentExtractor.extractingIndex;

public class DemoTest {

    public static void main(String[] args) {

        // Maintains a count of the number of excerpts encountered
        Reduction<LongSupplier> counting = Reductions.counting();
        // ...
        long count = counting.reduction().getAsLong();

        // Maintains a view of the highest index encountered or 0 if no index was encountered
        Reduction<LongSupplier> maxIndexing =
                Reductions.reducingLong(extractingIndex(), 0L, Math::max);

        long maxIndex = maxIndexing.reduction().getAsLong();

        // Maintains a List of all MarketData elements encountered in a List
        Reduction<List<MarketData>> listing =
                Reduction.of(builder(MarketData.class).build())
                        .collecting(ConcurrentCollectors.toConcurrentList());

        // Maintains a Set of all MarketData symbols starting with "S"
        Reduction<Set<String>> symbolsStartingWithS = Reduction.of(
                        builder(MarketData.class).withReusing(MarketData::new).build()
                                .map(MarketData::symbol)
                                .filter(s -> s.startsWith("S")))
                .collecting(ConcurrentCollectors.toConcurrentSet());

        // Maintains a Map of the latest MarketData message per symbol where the
        // messages were previously written by a MethodWriter of type MarketDataProvider
        Reduction<Map<String, MarketData>> latest = Reduction.of(
                        DocumentExtractor.builder(MarketData.class)
                                .withMethod(MarketDataProvider.class, MarketDataProvider::marketData)
                                .build())
                .collecting(Collectors.toConcurrentMap(
                        MarketData::symbol,
                        Function.identity(),
                        replacingMerger())
                );

        MarketData latestAppleMarketData = latest.reduction().get("AAPL");

        // This creates a live view of the reduction.
        Map<String, MarketData> liveQueueBackedMap = latest.reduction();


        // Maintains a protected Map of the latest MarketData message per symbol where the
        // messages were previously written by a MethodWriter of type MarketDataProvider
        Reduction<Map<String, MarketData>> latestProtected = Reduction.of(
                        DocumentExtractor.builder(MarketData.class)
                                .withMethod(MarketDataProvider.class, MarketDataProvider::marketData)
                                .build())
                .collecting(Collectors.collectingAndThen(
                                Collectors.toConcurrentMap(
                                        MarketData::symbol,
                                        Function.identity(),
                                        replacingMerger()),
                                Collections::unmodifiableMap
                        )
                );


        // Maintains statistics per symbol on MarketData::last using vanilla Java
        // classes (creates objects).
        Reduction<ConcurrentMap<String, DoubleSummaryStatistics>> stats = Reduction.of(
                        DocumentExtractor.builder(MarketData.class)
                                .withMethod(MarketDataProvider.class, MarketDataProvider::marketData)
                                .build())
                .collecting(groupingByConcurrent(
                                MarketData::symbol,
                                summarizingDouble(MarketData::last)
                        )
                );

        double averageApplePrice = stats.reduction().get("AAPL").getAverage();


        // This is a demo of a queue-backed map that is continuously listening to a queue
        // and any change is reflected in the Reduction.

        Reduction<Map<String, MarketData>> queueBackedMapping = Reduction.of(
                        DocumentExtractor.builder(MarketData.class)
                                .withMethod(MarketDataProvider.class, MarketDataProvider::marketData)
                                .build())
                .collecting(Collectors.collectingAndThen(
                                Collectors.toConcurrentMap(
                                        MarketData::symbol,
                                        Function.identity(),
                                        replacingMerger()
                                ),
                                Collections::unmodifiableMap
                        )
                );

        // This provides a concurrent automatically updated view of the queue-backed map.
        Map<String, MarketData> queueBackedMap = queueBackedMapping.reduction();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try (AutoTailers.CloseableRunnable runnable = AutoTailers.createRunnable(
                CreateUtil::create,
                queueBackedMapping,
                PauserMode.balanced
        )) {
            executorService.submit(runnable);
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        } catch (InterruptedException ie) {
            // do nothing
        }
        net.openhft.chronicle.threads.Threads.shutdown(executorService);


        // How to use thread confined objects?

    }


    public interface MarketDataProvider {

        void marketData(MarketData marketData);

        void news(String news);

        void action(Action action);

        enum Action {
            OPEN, CLOSE;
        }


    }

}
