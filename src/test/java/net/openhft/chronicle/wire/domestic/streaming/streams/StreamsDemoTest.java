package net.openhft.chronicle.wire.domestic.streaming.streams;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.stream.Streams;
import net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor;
import net.openhft.chronicle.wire.domestic.streaming.reduction.MarketData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.wire.domestic.streaming.CreateUtil.*;
import static net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StreamsDemoTest {

    @Test
    void streamTypeMarketDataSimple() {
        ClassAliasPool.CLASS_ALIASES.addAlias(MarketData.class);
        MarshallableIn wire = createThenValueOuts(
                vo -> vo.object(new MarketData("MSFT", 100, 110, 90)),
                vo -> vo.object(new MarketData("AAPL", 200, 220, 180)),
                vo -> vo.object(new MarketData("MSFT", 101, 110, 90))
        );

        String s = Streams.of(
                        wire,
                        builder(MarketData.class).build()
                )
                .skip(0)
                // skip 100
                .limit(50)
                .map(Object::toString)
                .collect(Collectors.joining(","));

        assertEquals("!MarketData {\n" +
                "  symbol: MSFT,\n" +
                "  last: 100.0,\n" +
                "  high: 110.0,\n" +
                "  low: 90.0\n" +
                "}\n" +
                ",!MarketData {\n" +
                "  symbol: AAPL,\n" +
                "  last: 200.0,\n" +
                "  high: 220.0,\n" +
                "  low: 180.0\n" +
                "}\n" +
                ",!MarketData {\n" +
                "  symbol: MSFT,\n" +
                "  last: 101.0,\n" +
                "  high: 110.0,\n" +
                "  low: 90.0\n" +
                "}\n", s);
    }

    @Test
    void latestIndex() {
        MarshallableIn in = createThenValueOuts(
                vo -> vo.writeLong(1),
                vo -> vo.writeLong(2),
                vo -> vo.writeLong(3)
        );
        long last = Streams.ofLong(in, ToLongDocumentExtractor.extractingIndex())
                .peek(System.out::println)
                .max()
                .orElse(-1);

        assertEquals(14, last);
    }


    @Test
    void streamRaw() {
        MarshallableIn in = createThenValueOuts(
                vo -> vo.writeLong(1),
                vo -> vo.writeLong(2),
                vo -> vo.writeLong(3)
        );
        LongSummaryStatistics stat = Streams.ofLong(in,
                        (wire, index) -> wire.getValueIn().readLong())
                .summaryStatistics();

        LongSummaryStatistics expected = LongStream.of(1, 2, 3).summaryStatistics();
        assertLongSummaryStatisticsEqual(expected, stat);
    }

    @Test
    void streamTypeMarketData() {

        MarshallableIn in = createThenValueOuts(
                vo -> vo.object(new MarketData("MSFT", 100, 110, 90)),
                vo -> vo.object(new MarketData("APPL", 200, 220, 180)),
                vo -> vo.object(new MarketData("MSFT", 101, 110, 90))
        );


        Map<String, List<MarketData>> groups = Streams.of(in, builder(MarketData.class).build())
                .collect(groupingBy(MarketData::symbol));

        Map<String, List<MarketData>> expected = Stream.of(
                        new MarketData("MSFT", 100, 110, 90),
                        new MarketData("APPL", 200, 220, 180),
                        new MarketData("MSFT", 101, 110, 90)
                )
                .collect(groupingBy(MarketData::symbol));

        assertEquals(expected, groups);

    }

    @Test
    void testIterator() {

        MarshallableIn in = createThenValueOuts(
                vo -> vo.object(new MarketData("MSFT", 100, 110, 90)),
                vo -> vo.object(new MarketData("APPL", 200, 220, 180)),
                vo -> vo.object(new MarketData("MSFT", 101, 110, 90))
        );

        DoubleAdder adder = new DoubleAdder();
        Iterator<MarketData> iterator = Streams.iterator(in, builder(MarketData.class).build());
        iterator.forEachRemaining(md -> adder.add(md.last()));

        assertEquals(401.0, adder.doubleValue(), 1e-10);
    }


    @Test
    void streamType2() {
        MarshallableIn in = createThenValueOuts(
                vo -> vo.object(new Shares("ABCD", 100_000_000)),
                vo -> vo.object(new Shares("EFGH", 200_000_000)),
                vo -> vo.object(new Shares("ABCD", 300_000_000))
        );

        Map<String, List<Shares>> groups = Streams.of(in, builder(Shares.class).build())
                .collect(groupingBy(Shares::symbol));

        Map<String, List<Shares>> expected = new HashMap<>();
        expected.put("ABCD", Arrays.asList(new Shares("ABCD", 100_000_000), new Shares("ABCD", 300_000_000)));
        expected.put("EFGH", Collections.singletonList(new Shares("EFGH", 200_000_000)));
        assertEquals(expected, groups);
    }

    @Test
    void streamMessageWriter() {
        News firstNews = new News("MSFT", "Microsoft releases Linux Windows", "In a stunning presentation today, ...");
        News secondNews = new News("APPL", "Apple releases Iphone 23", "Today, Apple released ...");

        MarshallableIn in = createThen(appender -> {
            Messages messages = appender.methodWriter(Messages.class);
            messages.news(firstNews);
            messages.shares(new Shares("AGDG", 100_000_000));
            messages.news(secondNews);
            messages.shares(new Shares("AGDG", 200_000_000));
        });

        List<News> newsList = Streams.of(
                        in,
                        builder(News.class).withMethod(Messages.class, Messages::news).build()
                )
                .sorted(Comparator.comparing(News::symbol))
                .collect(toList());

        List<News> expected = Stream.of(firstNews, secondNews)
                .sorted(Comparator.comparing(News::symbol))
                .collect(toList());
        assertEquals(expected, newsList);

        final LongSummaryStatistics stat = Streams.of(
                        resetted(in),
                        builder(Shares.class).withMethod(Messages.class, Messages::shares).build()
                )
                .mapToLong(Shares::noShares)
                .summaryStatistics();

        LongSummaryStatistics expectedStat = LongStream.of(100_000_000, 200_000_000).summaryStatistics();
        assertLongSummaryStatisticsEqual(expectedStat, stat);

        final LongSummaryStatistics stat2 = Streams.ofLong(resetted(in),
                        builder(Shares.class).
                                withMethod(Messages.class, Messages::shares)
                                .withReusing(Shares::new)
                                .build()
                                .mapToLong(Shares::noShares)
                )
                .summaryStatistics();

        assertLongSummaryStatisticsEqual(expectedStat, stat2);
    }


    @Test
    @Disabled("Parallel is not supported yet")
    void longStreamParallel() {
        final int no = 100_000;

        Wire in = create();


        for (int i = 0; i < no; i++) {
            try (DocumentContext dc = in.writingDocument()) {
                dc.wire()
                        .getValueOut()
                        .writeLong(i);
            }
        }

        Set<Thread> threads = Collections.newSetFromMap(new ConcurrentHashMap<>());

        long sum = Streams.ofLong(in,
                        (wire, index) -> wire.getValueIn().readLong())
                .parallel()
                .peek(v -> threads.add(Thread.currentThread()))
                .sum();

        long expected = (long) no * no / 2L;
        assertEquals(expected, sum);

        System.out.println("threads = " + threads);

    }

    @Disabled("Does not work properly since net.openhft.chronicle.bytes.NativeBytes is not thread-safe")
    @Test
    void streamParallel() {
        final int no = 100_000;

        Wire in = create();

        for (int i = 0; i < no; i++) {
            try (DocumentContext dc = in.writingDocument()) {
                dc.wire()
                        .getValueOut()
                        .object(new Shares("AGDG", i));
            }
        }

        Set<Thread> threads = Collections.newSetFromMap(new ConcurrentHashMap<>());

        long sum = Streams.of(in,
                        builder(Shares.class).build())
                .parallel()
                .peek(v -> threads.add(Thread.currentThread()))
                .mapToLong(Shares::noShares)
                .sum();

        long expected = (long) (no - 1) * no / 2L;
        assertEquals(expected, sum);

        if (Runtime.getRuntime().availableProcessors() > 2) {
            assertTrue(threads.size() > 1);
        }

    }

    @Test
    @Disabled("Performance test")
    void performance() {
        final int no = 100_000;

        Wire in = create();

        for (int i = 0; i < no; i++) {
            try (DocumentContext dc = in.writingDocument()) {
                dc.wire()
                        .getValueOut()
                        .object(new Shares("AGDG", i));
            }
        }

        long st = 0;
        long it = 0;
        for (int i = 0; i < 105; i++) {

            MarshallableIn tailer = resetted(in);

            final long iterationBegin = System.currentTimeMillis();
            final Shares shares = new Shares();
            long sum = 0;
            for (; ; ) {
                try (DocumentContext dc = tailer.readingDocument()) {
                    if (dc.isPresent()) {
                        Wire wire = dc.wire();
                        if (wire != null) {
                            Shares s = wire.getValueIn().object(shares, Shares.class);
                            sum += s.noShares();
                            continue;
                        }
                    }
                    break;
                }
            }

            final long iterationDurationMs = System.currentTimeMillis() - iterationBegin;

            final long streamBegin = System.currentTimeMillis();

            long streamSum = Streams.ofLong(resetted(in),
                            builder(Shares.class)
                                    .withReusing(Shares::new)
                                    .build()
                                    .mapToLong(Shares::noShares))
                    .sum();

            final long streamDurationMs = System.currentTimeMillis() - streamBegin;

            assertEquals(streamSum, sum);
            System.out.println("streamDurationMs = " + streamDurationMs);
            System.out.println("iterationDurationMs = " + iterationDurationMs);

            if (i > 5) {
                st += streamDurationMs;
                it += iterationDurationMs;
            }

        }
        System.out.println("st = " + st);
        System.out.println("it = " + it);

    }

    @Test
    void streamCloseTailer() {
        MarshallableIn in = createThenValueOuts(
                vo -> vo.object(new MarketData("MSFT", 100, 110, 90)),
                vo -> vo.object(new MarketData("APPL", 200, 220, 180)),
                vo -> vo.object(new MarketData("MSFT", 101, 110, 90))
        );

        Map<String, List<MarketData>> groups;

        groups = Streams.of(in, builder(MarketData.class).build())
                .collect(groupingBy(MarketData::symbol));

        // A bit sloppy...
        assertEquals(2, groups.size());
    }

    @Test
    void streamObjectReuse() {
        MarshallableIn in = createThenValueOuts(
                vo -> vo.object(new MarketData("MSFT", 100, 110, 90)),
                vo -> vo.object(new MarketData("AAPL", 200, 220, 180)),
                vo -> vo.object(new MarketData("MSFT", 101, 110, 90))
        );

        OptionalDouble max = Streams.of(in,
                        builder(MarketData.class)
                                .withReusing(MarketData::new)
                                .build())
                .mapToDouble(MarketData::last)
                .max();

        OptionalDouble expected = OptionalDouble.of(200);

        assertEquals(expected, max);
    }

    @Test
    void streamIllegalObjectReuse() {
        MarshallableIn in = createThenValueOuts(
                vo -> vo.object(new MarketData("MSFT", 100, 110, 90)),
                vo -> vo.object(new MarketData("AAPL", 200, 220, 180)),
                vo -> vo.object(new MarketData("MSFT", 101, 110, 90))
        );

        List<MarketData> list = Streams.of(in,
                        builder(MarketData.class)
                                .withReusing(MarketData::new)
                                .build())
                .collect(toList());

        // We will see the last entry in all positions
        List<MarketData> expected = Stream.of(
                        new MarketData("MSFT", 101, 110, 90),
                        new MarketData("MSFT", 101, 110, 90),
                        new MarketData("MSFT", 101, 110, 90)
                )
                .collect(toList());

        assertEquals(expected, list);
    }


    public interface Messages {

        void shares(Shares shares);

        void news(News news);

        void greeting(String greeting);

    }

    public static final class Shares extends SelfDescribingMarshallable {

        private String symbol;
        private long noShares;

        public Shares() {
        }

        public Shares(@NotNull String symbol, long noShares) {
            this.symbol = symbol;
            this.noShares = noShares;
        }

        public String symbol() {
            return symbol;
        }

        public void symbol(String symbol) {
            this.symbol = symbol;
        }

        public long noShares() {
            return noShares;
        }

        public void noShares(long shares) {
            this.noShares = shares;
        }
    }

    public static final class News extends SelfDescribingMarshallable {

        private String symbol;
        private String header;
        private String body;

        public News() {
        }

        public News(String symbol, String header, String body) {
            this.symbol = symbol;
            this.header = header;
            this.body = body;
        }

        public String symbol() {
            return symbol;
        }

        public void symbol(String symbol) {
            this.symbol = symbol;
        }

        public String header() {
            return header;
        }

        public void header(String header) {
            this.header = header;
        }

        public String body() {
            return body;
        }

        public void body(String body) {
            this.body = body;
        }

    }

    private static void assertLongSummaryStatisticsEqual(LongSummaryStatistics a,
                                                         LongSummaryStatistics b) {

        assertTrue(Stream.<Function<LongSummaryStatistics, Object>>of(
                LongSummaryStatistics::getCount,
                LongSummaryStatistics::getMin,
                LongSummaryStatistics::getMax,
                LongSummaryStatistics::getSum,
                LongSummaryStatistics::getAverage
        ).allMatch(op -> op.apply(a).equals(op.apply(b))));
    }

    private MarshallableIn resetted(MarshallableIn in) {
        ((Wire) in).bytes().readPosition(0);
        return in;
    }


}