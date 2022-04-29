package net.openhft.chronicle.wire.internal.streaming.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ExcerptListener;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.internal.streaming.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.*;
import java.util.stream.Collector;

import static java.util.Objects.requireNonNull;

public final class ReductionUtil {

    // Suppresses default constructor, ensuring non-instantiability.
    private ReductionUtil() {
    }

    public static long accept(@NotNull final MarshallableIn tailer,
                              @NotNull final ExcerptListener excerptListener) {
        requireNonNull(tailer);
        long lastIndex = -1;
        boolean end = false;
        while (!end) {
            try (final DocumentContext dc = tailer.readingDocument()) {
                final Wire wire = dc.wire();
                if (dc.isPresent() && wire != null) {
                    lastIndex = dc.index();
                    excerptListener.onExcerpt(wire, lastIndex);
                } else {
                    end = true;
                }
            }
        }
        return lastIndex;
    }

    public static final class CollectorReduction<E, A, R> implements Reduction<R> {
        private final DocumentExtractor<E> extractor;
        private final Collector<E, A, ? extends R> collector;

        private final A accumulation;

        public CollectorReduction(@NotNull final DocumentExtractor<E> extractor,
                                  @NotNull final Collector<E, A, ? extends R> collector) {
            this.extractor = extractor;
            this.collector = collector;

            if (!collector.characteristics().contains(Collector.Characteristics.CONCURRENT)) {
                Jvm.warn().on(CollectorReduction.class, "The collector " + collector + " should generally have the characteristics CONCURRENT");
            }
            this.accumulation = collector.supplier().get();
        }

        @Override
        public void onExcerpt(@NotNull Wire wire, long index) {
            final E element = extractor.extract(wire, index);
            if (element != null) {
                collector.accumulator()
                        .accept(accumulation, element);
            }
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public R reduction() {
            if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                return (R) accumulation;
            }
            return collector.finisher().apply(accumulation);
        }

        @Override
        public long accept(@NotNull final MarshallableIn tailer) {
            requireNonNull(tailer);
            return ReductionUtil.accept(tailer, this);
        }
    }

    public static final class LongSupplierReduction<A> implements Reduction<LongSupplier> {
        private final ToLongDocumentExtractor extractor;
        private final ObjLongConsumer<A> accumulator;
        private final A accumulation;
        private final ToLongFunction<A> finisher;

        public LongSupplierReduction(@NotNull final ToLongDocumentExtractor extractor,
                                     @NotNull final Supplier<A> supplier,
                                     @NotNull final ObjLongConsumer<A> accumulator,
                                     @NotNull final ToLongFunction<A> finisher) {
            this.extractor = requireNonNull(extractor);
            this.accumulator = requireNonNull(accumulator);
            requireNonNull(supplier);
            this.accumulation = requireNonNull(supplier.get());
            this.finisher = requireNonNull(finisher);
        }

        @Override
        public void onExcerpt(@NotNull Wire wire, long index) {
            final long element = extractor.extractAsLong(wire, index);
            if (element != Long.MIN_VALUE) {
                accumulator.accept(accumulation, element);
            }
        }

        @NotNull
        @Override
        public LongSupplier reduction() {
            return () -> finisher.applyAsLong(accumulation);
        }

        @Override
        public long accept(@NotNull final MarshallableIn tailer) {
            requireNonNull(tailer);
            return ReductionUtil.accept(tailer, this);
        }
    }

    public static final class DoubleSupplierReduction<A> implements Reduction<DoubleSupplier> {
        private final ToDoubleDocumentExtractor extractor;
        private final ObjDoubleConsumer<A> accumulator;
        private final A accumulation;
        private final ToDoubleFunction<A> finisher;

        public DoubleSupplierReduction(@NotNull final ToDoubleDocumentExtractor extractor,
                                       @NotNull final Supplier<A> supplier,
                                       @NotNull final ObjDoubleConsumer<A> accumulator,
                                       @NotNull final ToDoubleFunction<A> finisher) {
            this.extractor = requireNonNull(extractor);
            this.accumulator = requireNonNull(accumulator);
            requireNonNull(supplier);
            this.accumulation = requireNonNull(supplier.get());
            this.finisher = requireNonNull(finisher);
        }

        @Override
        public void onExcerpt(@NotNull Wire wire, long index) {
            final double element = extractor.extractAsDouble(wire, index);
            if (!Double.isNaN(element)) {
                accumulator.accept(accumulation, element);
            }
        }

        @NotNull
        @Override
        public DoubleSupplier reduction() {
            return () -> finisher.applyAsDouble(accumulation);
        }

        @Override
        public long accept(@NotNull final MarshallableIn tailer) {
            requireNonNull(tailer);
            return ReductionUtil.accept(tailer, this);
        }
    }

    // Reduction Builders

    public static final class VanillaReductionBuilder<E> implements Reduction.ReductionBuilder<E> {

        private final DocumentExtractor<E> extractor;

        public VanillaReductionBuilder(@NotNull final DocumentExtractor<E> extractor) {
            this.extractor = extractor;
        }

        @Override
        public <A, R> Reduction<R> collecting(@NotNull Collector<E, A, ? extends R> collector) {
            ObjectUtils.requireNonNull(collector);
            return new ReductionUtil.CollectorReduction<>(extractor, collector);
        }
    }

    public static final class VanillaLongReductionBuilder implements Reduction.LongReductionBuilder {

        private final ToLongDocumentExtractor extractor;

        public VanillaLongReductionBuilder(@NotNull final ToLongDocumentExtractor extractor) {
            this.extractor = extractor;
        }

        @Override
        public <A> Reduction<LongSupplier> reducing(@NotNull final Supplier<A> supplier,
                                                    @NotNull final ObjLongConsumer<A> accumulator,
                                                    @NotNull final ToLongFunction<A> finisher) {
            ObjectUtils.requireNonNull(supplier);
            ObjectUtils.requireNonNull(accumulator);
            ObjectUtils.requireNonNull(finisher);
            return new ReductionUtil.LongSupplierReduction<>(extractor, supplier, accumulator, finisher);
        }
    }

    public static final class VanillaDoubleReductionBuilder implements Reduction.DoubleReductionBuilder {

        private final ToDoubleDocumentExtractor extractor;

        public VanillaDoubleReductionBuilder(@NotNull final ToDoubleDocumentExtractor extractor) {
            this.extractor = extractor;
        }

        @Override
        public <A> Reduction<DoubleSupplier> reducing(@NotNull final Supplier<A> supplier,
                                                      @NotNull final ObjDoubleConsumer<A> accumulator,
                                                      @NotNull final ToDoubleFunction<A> finisher) {
            ObjectUtils.requireNonNull(supplier);
            ObjectUtils.requireNonNull(accumulator);
            ObjectUtils.requireNonNull(finisher);
            return new ReductionUtil.DoubleSupplierReduction<>(extractor, supplier, accumulator, finisher);
        }
    }

}