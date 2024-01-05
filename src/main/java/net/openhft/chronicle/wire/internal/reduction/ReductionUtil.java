/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.internal.reduction;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ExcerptListener;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.extractor.ToDoubleDocumentExtractor;
import net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import org.jetbrains.annotations.NotNull;

import java.util.function.*;
import java.util.stream.Collector;

import static java.util.Objects.requireNonNull;

/**
 * The ReductionUtil class provides utility methods and inner classes related to the reduction process.
 * It offers functionalities to work with marshallable objects and listeners to read excerpts
 * from documents and perform various reduction operations on them.
 */
public final class ReductionUtil {

    // Suppresses default constructor, ensuring non-instantiability.
    private ReductionUtil() {
    }

    /**
     * Reads excerpts from the given tailer until no more are present. For each excerpt, the given listener is invoked.
     *
     * @param tailer The MarshallableIn instance to read excerpts from.
     * @param excerptListener The listener to notify for each excerpt.
     * @return The index of the last read excerpt.
     * @throws InvalidMarshallableException if an issue occurs during reading.
     */
    public static long accept(@NotNull final MarshallableIn tailer,
                              @NotNull final ExcerptListener excerptListener) throws InvalidMarshallableException {
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

    /**
     * The CollectorReduction class provides functionalities to perform reduction operations using
     * a specific collector and an extractor. It extracts data from a wire, collects it using the
     * provided collector, and then performs the final reduction.
     *
     * @param <E> Type of the element to extract.
     * @param <A> Intermediate accumulation type of the collector.
     * @param <R> Result type of the reduction.
     */
    public static final class CollectorReduction<E, A, R> implements Reduction<R> {

        // Extracts elements of type E from a wire.
        private final DocumentExtractor<E> extractor;

        // The collector to accumulate elements and produce a final result.
        private final Collector<E, A, ? extends R> collector;

        // The current accumulated value.
        private final A accumulation;

        /**
         * Initializes a new instance of CollectorReduction with the provided extractor and collector.
         *
         * @param extractor The extractor to fetch elements from a wire.
         * @param collector The collector to accumulate and reduce the elements.
         */
        public CollectorReduction(@NotNull final DocumentExtractor<E> extractor,
                                  @NotNull final Collector<E, A, ? extends R> collector) {
            this.extractor = extractor;
            this.collector = collector;

            // Warn if the collector doesn't have CONCURRENT characteristic.
            if (!collector.characteristics().contains(Collector.Characteristics.CONCURRENT)) {
                Jvm.warn().on(CollectorReduction.class, "The collector " + collector + " should generally have the characteristics CONCURRENT");
            }
            this.accumulation = collector.supplier().get();
        }

        @Override
        public void onExcerpt(@NotNull Wire wire, long index) throws InvalidMarshallableException {
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
        public long accept(@NotNull final MarshallableIn tailer) throws InvalidMarshallableException {
            requireNonNull(tailer);
            return ReductionUtil.accept(tailer, this);
        }
    }

    /**
     * The LongSupplierReduction class provides functionalities to perform reduction operations
     * over long values extracted from documents. It reads long values from a wire, accumulates them,
     * and then offers a final reduction in the form of a LongSupplier.
     *
     * @param <A> Intermediate accumulation type.
     */
    public static final class LongSupplierReduction<A> implements Reduction<LongSupplier> {

        // Extracts long values from a wire.
        private final ToLongDocumentExtractor extractor;

        // The accumulator to accumulate long values.
        private final ObjLongConsumer<A> accumulator;

        // The current accumulated value.
        private final A accumulation;

        // Function to finish the reduction and produce a result.
        private final ToLongFunction<A> finisher;

        /**
         * Initializes a new instance of LongSupplierReduction with the provided extractor, supplier,
         * accumulator, and finisher.
         *
         * @param extractor The extractor to fetch long values from a wire.
         * @param supplier The supplier to initialize the accumulation.
         * @param accumulator The accumulator to accumulate long values.
         * @param finisher The function to finish the reduction.
         */
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
        public void onExcerpt(@NotNull Wire wire, long index) throws InvalidMarshallableException {
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
        public long accept(@NotNull final MarshallableIn tailer) throws InvalidMarshallableException {
            requireNonNull(tailer);
            return ReductionUtil.accept(tailer, this);
        }
    }

    /**
     * The DoubleSupplierReduction class provides functionalities to perform reduction operations
     * over double values extracted from documents. It reads double values from a wire, accumulates them,
     * and then offers a final reduction in the form of a DoubleSupplier.
     *
     * @param <A> Intermediate accumulation type.
     */
    public static final class DoubleSupplierReduction<A> implements Reduction<DoubleSupplier> {

        // Extracts double values from a wire.
        private final ToDoubleDocumentExtractor extractor;

        // The accumulator to accumulate double values.
        private final ObjDoubleConsumer<A> accumulator;

        // The current accumulated value.
        private final A accumulation;

        // Function to finish the reduction and produce a result.
        private final ToDoubleFunction<A> finisher;

        /**
         * Initializes a new instance of DoubleSupplierReduction with the provided extractor, supplier,
         * accumulator, and finisher.
         *
         * @param extractor The extractor to fetch double values from a wire.
         * @param supplier The supplier to initialize the accumulation.
         * @param accumulator The accumulator to accumulate double values.
         * @param finisher The function to finish the reduction.
         */
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
        public void onExcerpt(@NotNull Wire wire, long index) throws InvalidMarshallableException {
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
        public long accept(@NotNull final MarshallableIn tailer) throws InvalidMarshallableException {
            requireNonNull(tailer);
            return ReductionUtil.accept(tailer, this);
        }
    }

    // Reduction Builders

    /**
     * The VanillaReductionBuilder class assists in building reductions based on the extraction of elements of type E.
     * It leverages provided DocumentExtractors to aid in this process.
     *
     * @param <E> The type of element to be extracted from documents.
     */
    public static final class VanillaReductionBuilder<E> implements Reduction.ReductionBuilder<E> {

        // Extracts elements of type E from a wire/document.
        private final DocumentExtractor<E> extractor;

        /**
         * Initializes a new instance of VanillaReductionBuilder with the provided extractor.
         *
         * @param extractor The extractor to fetch elements of type E from a wire.
         */
        public VanillaReductionBuilder(@NotNull final DocumentExtractor<E> extractor) {
            this.extractor = extractor;
        }

        @Override
        public <A, R> Reduction<R> collecting(@NotNull Collector<E, A, ? extends R> collector) {
            ObjectUtils.requireNonNull(collector);
            // Create a new CollectorReduction using the provided collector and this builder's extractor.
            return new ReductionUtil.CollectorReduction<>(extractor, collector);
        }
    }

    /**
     * The VanillaLongReductionBuilder class assists in building reductions that work with long values.
     * It leverages provided ToLongDocumentExtractors to aid in this process.
     */
    public static final class VanillaLongReductionBuilder implements Reduction.LongReductionBuilder {

        // Extracts long values from a wire/document.
        private final ToLongDocumentExtractor extractor;

        /**
         * Initializes a new instance of VanillaLongReductionBuilder with the provided long extractor.
         *
         * @param extractor The extractor to fetch long values from a wire.
         */
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
