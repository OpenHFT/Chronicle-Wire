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

package net.openhft.chronicle.wire.domestic.reduction;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.extractor.ToDoubleDocumentExtractor;
import net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor;
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

/**
 * This is the Reductions utility class.
 * It provides static methods to create various types of Reductions, offering functionalities
 * related to longs, doubles, and counting excerpts.
 *
 * @since 2023-09-16
 */
public final class Reductions {

    // Suppresses default constructor, ensuring non-instantiability.
    private Reductions() {
    }

    // Specialized Reductions

    /**
     * Generates a specialized Reduction tailored for long values.
     * This method utilizes the provided extractor for retrieving long values,
     * then aggregates those values using the specified accumulator.
     * The aggregation starts with an initial value, denoted by the identity parameter.
     *
     * @param extractor   A function to extract long values from documents (must not be null)
     * @param identity    The initial value for the aggregation
     * @param accumulator A binary operator used for aggregation (must not be null)
     * @return A Reduction optimized for aggregating long values
     * @throws NullPointerException if either extractor or accumulator is null.
     */
    public static Reduction<LongSupplier> reducingLong(@NotNull final ToLongDocumentExtractor extractor,
                                                       final long identity,
                                                       @NotNull final LongBinaryOperator accumulator) {
        requireNonNull(extractor); // Ensure the extractor is not null
        requireNonNull(accumulator); // Ensure the accumulator is not null

        // Construct and return the Reduction
        return Reduction.ofLong(extractor)
                .reducing(
                        () -> new LongAccumulator(accumulator, identity),
                        LongAccumulator::accumulate,
                        LongAccumulator::get
                );
    }

    /**
     * Generates a specialized Reduction tailored for double values.
     * This method employs the given extractor to obtain double values,
     * and subsequently aggregates those values using the accumulator provided.
     * The aggregation process commences with the identity as its initial value.
     *
     * @param extractor   A function to extract double values from documents (must not be null)
     * @param identity    The starting value for the aggregation
     * @param accumulator A binary operator designed for aggregating double values (must not be null)
     * @return A Reduction optimized for aggregating double values
     * @throws NullPointerException if either extractor or accumulator is null.
     */
    public static Reduction<DoubleSupplier> reducingDouble(@NotNull final ToDoubleDocumentExtractor extractor,
                                                           final double identity,
                                                           @NotNull final DoubleBinaryOperator accumulator) {
        requireNonNull(extractor); // Ensure the extractor is not null
        requireNonNull(accumulator); // Ensure the accumulator is not null

        // Construct and return the Reduction
        return Reduction.ofDouble(extractor)
                .reducing(
                        () -> new DoubleAccumulator(accumulator, identity),
                        DoubleAccumulator::accumulate,
                        DoubleAccumulator::get
                );
    }

    /**
     * Constructs a Reduction geared towards counting the number of excerpts.
     * This Reduction is optimized to avoid creating any internal objects during its operation.
     *
     * @return A Reduction that counts the number of excerpts processed
     */
    public static Reduction<LongSupplier> counting() {
        // Create and return the counting Reduction
        return Reduction.ofLong(
                        (wire, index) -> 1L)
                .reducing(
                        LongAdder::new,
                        LongAdder::add,
                        LongAdder::sum
                );
    }

    /**
     * The Counting class is a Reduction implementation designed to count the number of excerpts processed.
     * It possesses configurable properties, making it adaptable for various configurations,
     * including those specified in YAML files.
     */
    public static final class Counting extends SelfDescribingMarshallable implements Reduction<LongSupplier> {

        // An atomic field updater to provide thread-safe updates to the counter
        private static final AtomicLongFieldUpdater<Counting> UPDATER =
                AtomicLongFieldUpdater.newUpdater(Counting.class, "counter");

        // A volatile counter to ensure atomic read/write operations across multiple threads
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
