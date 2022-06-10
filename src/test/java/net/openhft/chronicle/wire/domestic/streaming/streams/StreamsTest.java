package net.openhft.chronicle.wire.domestic.streaming.streams;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.testframework.Combination;
import net.openhft.chronicle.testframework.Delegation;
import net.openhft.chronicle.testframework.Permutation;
import net.openhft.chronicle.testframework.Product;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.domestic.streaming.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.streaming.Streams;
import net.openhft.chronicle.wire.domestic.streaming.reduction.MarketData;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.TestFactory;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class StreamsTest {

    @TestFactory
    Stream<DynamicTest> test() {
        return Product.of(
                        // All the wire types we'd like to test
                        wireTypes().map(this::wire),
                        // All the various sequences of possible stream operations
                        Combination.of(operations())
                                .flatMap(Permutation::of)
                                // Use this type rather than the much longer Stream<List<Named<Function<Stream<... etc.
                                .map(NamedOperations::of),
                        // Constructor used for a nominal tuple of Wire and
                        WireOperationsRecord::new
                )
                .map(tuple -> DynamicTest.dynamicTest(tuple.toString(), () -> {

                    // As we reuse the wire, we need to clear it between each use
                    tuple.wire().bytes().readPosition(0);

                    // Apply all the operations on a reference stream using vanilla Stream operators
                    final Stream<MarketData> expected = tuple.operations.stream()
                            .reduce(marketDataStream(), (s, op) -> op.getPayload().apply(s), (a, b) -> a);

                    // Apply all the operations on a reference stream using the custom Stream operators
                    final Stream<MarketData> initialStream = Streams.of(
                            tuple.wire(),
                            DocumentExtractor.builder(MarketData.class).build());
                    final Stream<MarketData> actual = tuple.operations.stream()
                            .reduce(initialStream, (s, op) -> op.getPayload().apply(s), (a, b) -> a);

                    assertStreamEquals(expected, actual);
                }));
    }

    // Makes type declaration more concise
    interface NamedOperations extends List<Named<Function<Stream<MarketData>, Stream<MarketData>>>> {

        static NamedOperations of(List<Named<Function<Stream<MarketData>, Stream<MarketData>>>> operations) {
            return Delegation.of(operations)
                    .as(NamedOperations.class)
                    .toStringFunction(ops -> ops.stream()
                            .map(Named::getName)
                            .collect(joining(", ")))
                    .build();
        }

    }

    static final class WireOperationsRecord {
        private final Wire wire;
        private final NamedOperations operations;

        public WireOperationsRecord(Wire wire, NamedOperations operations) {
            this.wire = wire;
            this.operations = operations;
        }

        Wire wire() {
            return wire;
        }

        NamedOperations operations() {
            return operations;
        }

        @Override
        public String toString() {
            return wire.getClass().getSimpleName() + " ops: " + operations().toString();
        }
    }

    Stream<Named<Function<Stream<MarketData>, Stream<MarketData>>>> operations() {
        return Stream.of(
                Named.of("filter", s -> s.filter(md -> md.last() % 2 == 1)),
                Named.of("distinct", Stream::distinct),
                Named.of("sorted", s -> s.sorted(Comparator.comparing(MarketData::symbol))),
                // The map operation must map to the same type or else the test will not work
                Named.of("map", s -> s.map(md -> {
                    MarketData n = new MarketData(md);
                    n.low(md.low() - 1d);
                    return n;
                })),
                Named.of("limit(3)", s -> s.limit(3)),
                Named.of("skip(1)", s -> s.skip(1))
        );
    }

    private Stream<MarketData> marketDataStream() {
        return Stream.of(
                new MarketData("MSFT", 101, 110, 90),
                new MarketData("MSFT", 101, 110, 90),
                new MarketData("MSFT", 102, 110, 90),
                new MarketData("AAPL", 201, 210, 190),
                new MarketData("AAPL", 201, 210, 190),
                new MarketData("AAPL", 202, 210, 190)
        );
    }

    private Stream<Wire> wires() {
        return wireTypes()
                .map(this::wire);
    }

    private Stream<WireType> wireTypes() {
        return Stream.of(
                        WireType.TEXT,
                        WireType.JSON,
                        WireType.BINARY,
                        WireType.BINARY_LIGHT)
                .filter(WireType::isAvailable);
    }

    private Wire wire(WireType wireType) {
        final Bytes<?> bytes = Bytes.allocateDirect(1000);
        final Wire wire = wireType.apply(bytes);
        marketDataStream()
                .forEach(md -> {
                    try (DocumentContext dc = wire.writingDocument()) {
                        dc.wire().getValueOut().object(md);
                    }
                });
        return wire;
    }

    void assertStreamEquals(Stream<MarketData> expected, Stream<MarketData> actual) {
        final List<MarketData> expectedList = expected.collect(toList());
        final List<MarketData> actualList = actual
                // Make a copy as the stream might reuse objects
                .map(MarketData::new)
                .collect(toList());
        assertEquals(expectedList, actualList);
    }

}