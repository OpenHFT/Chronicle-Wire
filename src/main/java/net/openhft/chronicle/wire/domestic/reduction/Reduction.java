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

/**
 * This is the Reduction interface which extends ExcerptListener.
 * It provides a means to consume excerpts from a given wire and apply reductions on them.
 * The implementations of this interface should be thread-safe, especially if they are referenced as an {@link ExcerptListener}.
 */
public interface Reduction<T> extends ExcerptListener {

    /**
     * Consumes an excerpt from the provided {@code wire} at the specified {@code index}.
     * Care should be taken to minimize the probability of throwing Exceptions as they are relayed to the call site.
     * It's essential that implementations of this method remain thread-safe, especially when used as an {@link ExcerptListener}.
     *
     * @param wire  the wire containing the excerpt
     * @param index the index of the excerpt
     * @throws InvalidMarshallableException if there's an issue marshalling the provided wire
     */
    void onExcerpt(@NotNull Wire wire, @NonNegative long index) throws InvalidMarshallableException;

    /**
     * Provides a view of the underlying reduction. Implementations might return a snapshot
     * or a mutable view depending on the design.
     *
     * @return the current state or view of the reduction
     */
    @NotNull
    T reduction();

    /**
     * Processes and reduces the contents provided by the {@code tailer}.
     * The method can be used to initialize the reduction before appending new values.
     * It's the caller's responsibility to ensure no simultaneous appenders are accessing this reduction during the operation.
     *
     * @param tailer the source of the content to be reduced
     * @return the last index seen or -1 if no index was seen
     * @throws InvalidMarshallableException if there's an issue marshalling the tailer's content
     */
    default long accept(@NotNull final MarshallableIn tailer) throws InvalidMarshallableException {
        // Ensure the tailer is not null
        requireNonNull(tailer);
        return ReductionUtil.accept(tailer, this);
    }

    // Basic static constructors

    /**
     * Returns a ReductionBuilder for the given extractor which will extract elements of type E.
     *
     * @param extractor the extractor responsible for fetching elements of type E
     * @param <E>       the type of elements to be extracted
     * @return a ReductionBuilder designed for elements of type E
     */
    static <E> ReductionBuilder<E> of(@NotNull final DocumentExtractor<E> extractor) {
        // Ensure the extractor is not null
        requireNonNull(extractor);
        return new ReductionUtil.VanillaReductionBuilder<>(extractor);
    }

    /**
     * Returns a LongReductionBuilder for the given extractor which will extract elements of type long.
     *
     * @param extractor the extractor responsible for fetching elements of type long
     * @return a LongReductionBuilder designed for elements of type long
     */
    static LongReductionBuilder ofLong(@NotNull final ToLongDocumentExtractor extractor) {
        // Ensure the extractor is not null
        requireNonNull(extractor);
        return new ReductionUtil.VanillaLongReductionBuilder(extractor);
    }

    /**
     * Returns a DoubleReductionBuilder for the given extractor which will extract elements of type double.
     *
     * @param extractor the extractor responsible for fetching elements of type double
     * @return a DoubleReductionBuilder designed for elements of type double
     */
    static DoubleReductionBuilder ofDouble(@NotNull final ToDoubleDocumentExtractor extractor) {
        // Ensure the extractor is not null
        requireNonNull(extractor);
        return new ReductionUtil.VanillaDoubleReductionBuilder(extractor);
    }

    /**
     * ReductionBuilder is an interface that defines the contract for creating new Reductions using a specific collector.
     * Implementations of this interface should cater to the specific type of element being reduced.
     */
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

    /**
     * LongReductionBuilder is an interface that defines the contract for creating Reductions specialized for handling long data types.
     */
    interface LongReductionBuilder {

        /**
         * Creates and returns a new Reduction of type LongSupplier.
         * It uses the provided parameters to establish the reduction behavior, particularly the internal state and accumulation.
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

    /**
     * DoubleReductionBuilder is an interface that defines the contract for creating Reductions specialized for handling double data types.
     */
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
