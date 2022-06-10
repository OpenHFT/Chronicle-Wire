package net.openhft.chronicle.wire.reduction;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.extractor.ToDoubleDocumentExtractor;
import net.openhft.chronicle.wire.extractor.ToLongDocumentExtractor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongSupplier;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

public final class Reductions {

    // Suppresses default constructor, ensuring non-instantiability.
    private Reductions() {
    }

    // Specialized Reductions

    /**
     * Creates and returns a new Reduction that will extract elements of type {@code long} using
     * the provided {@code extractor} and will accumulate values using the provided {@code accumulator}.
     * The initial state of the reduction will be the provided {@code identity}.
     * <p>
     * The returned Reduction is guaranteed to not create any internal objects.
     *
     * @param extractor to apply on each document (non-null)
     * @param identity initial start value
     * @param accumulator to apply for each element (non-null)
     * @return a new Reduction reducing long values
     * @throws NullPointerException if any objects provided are {@code null}.
     */
    public static Reduction<LongSupplier> reducingLong(@NotNull final ToLongDocumentExtractor extractor,
                                                       final long identity,
                                                       @NotNull final LongBinaryOperator accumulator) {
        requireNonNull(extractor);
        requireNonNull(accumulator);

        return Reduction.ofLong(extractor)
                .reducing(
                        () -> new LongAccumulator(accumulator, identity),
                        LongAccumulator::accumulate,
                        LongAccumulator::get
                );
    }

    /**
     * Creates and returns a new Reduction that will extract elements of type {@code double} using
     * the provided {@code extractor} and will accumulate values using the provided {@code accumulator}.
     * The initial state of the reduction will be the provided {@code identity}.
     * <p>
     * The returned Reduction is guaranteed to not create any internal objects.
     *
     * @param extractor to apply on each document (non-null)
     * @param identity initial start value
     * @param accumulator to apply for each element (non-null)
     * @return a new Reduction reducing double values
     * @throws NullPointerException if any objects provided are {@code null}.
     */
    public static Reduction<DoubleSupplier> reducingDouble(@NotNull final ToDoubleDocumentExtractor extractor,
                                                           final double identity,
                                                           @NotNull final DoubleBinaryOperator accumulator) {
        requireNonNull(extractor);
        requireNonNull(accumulator);

        return Reduction.ofDouble(extractor)
                .reducing(
                        () -> new DoubleAccumulator(accumulator, identity),
                        DoubleAccumulator::accumulate,
                        DoubleAccumulator::get
                );
    }

    /**
     * Creates and returns a new Reduction that will count the number of excerpts.
     * <p>
     * The returned Reduction is guaranteed to not create any internal objects.
     *
     * @return a new Reduction counting excerpts
     */
    public static Reduction<LongSupplier> counting() {
        return Reduction.ofLong(
                        (wire, index) -> 1L)
                .reducing(
                        LongAdder::new,
                        LongAdder::add,
                        LongAdder::sum
                );
    }

    /**
     * A Reduction class that counts the number of excerpts that have been processed.
     * <p>
     * This is an example of a public class with configurable properties that can be
     * referenced in a YAML configuration file.
     */
    public static final class Counting extends SelfDescribingMarshallable implements Reduction<LongSupplier> {

        private static final AtomicLongFieldUpdater<Counting> UPDATER =
                AtomicLongFieldUpdater.newUpdater(Counting.class, "counter");

        private volatile long counter;

        @Override
        public void onExcerpt(@NotNull Wire wire, long index) {
            UPDATER.getAndIncrement(this);
        }

        @NotNull
        @Override
        public LongSupplier reduction() {
            return () -> counter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Counting that = (Counting) o;
            return this.counter == that.counter;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(counter);
        }
    }


}