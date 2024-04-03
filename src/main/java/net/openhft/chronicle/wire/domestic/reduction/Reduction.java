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

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.ExcerptListener;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.extractor.ToDoubleDocumentExtractor;
import net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor;
import net.openhft.chronicle.wire.internal.reduction.ReductionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.*;
import java.util.stream.Collector;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

public interface Reduction<T> extends ExcerptListener {

    /**
     * Consumes an excerpt from the provided {@code wire} at the index at the provided {@code index}.
     * <p>
     * If this method throws an Exception, it is relayed to the call site.
     * Therefore, care should be taken to minimise the probability of throwing Exceptions.
     * <p>
     * If this method is referenced as an {@link ExcerptListener} then the Reduction must be
     * thread-safe.
     **/
    void onExcerpt(@NotNull Wire wire, @NonNegative long index) throws InvalidMarshallableException;

    /**
     * Returns a view of the underlying reduction.
     *
     * @return Reduction view.
     */
    @NotNull
    T reduction();

    /**
     * Accepts the input of the provided {@code tailer } and reduces (folds) the contents of it
     * into this Reduction returning the last seen index or -1 if no index was seen.
     * <p>
     * This method can be used to initialise a Reduction before appending new values.
     * <p>
     * It is the responsibility of the caller to make sure no simultaneous appenders are using
     * this Reduction during the entire fold operation.
     *
     * @param tailer to reduce (fold) from
     * @return the last index seen or -1 if no index was seen
     * @throws NullPointerException if the provided {@code tailer} is {@code null}
     */
    default long accept(@NotNull final MarshallableIn tailer) throws InvalidMarshallableException {
        requireNonNull(tailer);
        return ReductionUtil.accept(tailer, this);
    }

    // Basic static constructors

    /**
     * Creates and returns a new {@code ReductionBuilder} that will use the provided
     * {@code extractor} to extract elements of type {@code E}.
     * <p>
     * This method initializes a generic reduction builder suitable for custom element types.
     *
     * @param <E>       the element type
     * @param extractor the document extractor used to extract elements, must not be null
     * @return a new {@code ReductionBuilder} instance
     * @see LongReductionBuilder
     * @see DoubleReductionBuilder
     */
    static <E> ReductionBuilder<E> of(@NotNull final DocumentExtractor<E> extractor) {
        requireNonNull(extractor);
        return new ReductionUtil.VanillaReductionBuilder<>(extractor);
    }

    /**
     * Creates and returns a new {@code LongReductionBuilder} that will use the provided
     * {@code extractor} to extract elements of type {@code long}.
     * <p>
     * This method initializes a reduction builder specifically for handling long values.
     *
     * @param extractor the document extractor for long values, must not be null
     * @return a new {@code LongReductionBuilder} instance
     * @see #ofLong(ToLongDocumentExtractor)
     * @see #ofDouble(ToDoubleDocumentExtractor)
     */
    static LongReductionBuilder ofLong(@NotNull final ToLongDocumentExtractor extractor) {
        requireNonNull(extractor);
        return new ReductionUtil.VanillaLongReductionBuilder(extractor);
    }

    /**
     * Creates and returns a new {@code DoubleReductionBuilder} that will use the provided
     * {@code extractor} to extract elements of type {@code double}.
     * <p>
     * This method initializes a reduction builder specifically for handling double values.
     *
     * @param extractor the document extractor for double values, must not be null
     * @return a new {@code DoubleReductionBuilder} instance
     * @see #of(DocumentExtractor)
     * @see #ofLong(ToLongDocumentExtractor)
     */
    static DoubleReductionBuilder ofDouble(@NotNull final ToDoubleDocumentExtractor extractor) {
        requireNonNull(extractor);
        return new ReductionUtil.VanillaDoubleReductionBuilder(extractor);
    }

    interface ReductionBuilder<E> {

        /**
         * Creates and returns a new Reduction of type R using the provided {@code collector}.
         * <p>
         * The provided {@code collector } should be concurrent.
         *
         * @param collector to use (non-null)
         * @param <A>       intermediate accumulation form
         * @param <R>       Reduction type
         * @return a new Reduction of type R
         * @throws NullPointerException if the provided {@code collector} is {@code null}
         */
        <A, R> Reduction<R> collecting(@NotNull final Collector<E, A, ? extends R> collector);
    }

    interface LongReductionBuilder {

        /**
         * Creates and returns a new Reduction of type LongSupplier using the provided
         * parameters.
         *
         * @param supplier    to use (non-null) to create the internal state
         * @param accumulator to use when merging long elements into the internal state
         * @param finisher    to use when extracting the result from the internal state
         * @param <A>         intermediate accumulation form (must be concurrent)
         * @return a new Reduction of type LongSupplier
         * @throws NullPointerException if any of the provided parameters are {@code null}
         */
        <A> Reduction<LongSupplier> reducing(@NotNull final Supplier<A> supplier,
                                             @NotNull final ObjLongConsumer<A> accumulator,
                                             @NotNull final ToLongFunction<A> finisher);

    }

    interface DoubleReductionBuilder {

        /**
         * Creates and returns a new Reduction of type DoubleSupplier using the provided
         * parameters.
         *
         * @param supplier    to use (non-null) to create the internal state
         * @param accumulator to use when merging double elements into the internal state
         * @param finisher    to use when extracting the result from the internal state
         * @param <A>         intermediate accumulation form (must be concurrent)
         * @return a new Reduction of type DoubleSupplier
         * @throws NullPointerException if any of the provided parameters are {@code null}
         */
        <A> Reduction<DoubleSupplier> reducing(@NotNull final Supplier<A> supplier,
                                               @NotNull final ObjDoubleConsumer<A> accumulator,
                                               @NotNull final ToDoubleFunction<A> finisher);

    }
}
