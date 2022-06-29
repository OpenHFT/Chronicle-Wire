package net.openhft.chronicle.wire.domestic.reduction;

import net.openhft.chronicle.core.annotation.NonNegative;
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
    void onExcerpt(@NotNull Wire wire, @NonNegative long index);

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
    default long accept(@NotNull final MarshallableIn tailer) {
        requireNonNull(tailer);
        return ReductionUtil.accept(tailer, this);
    }

    // Basic static constructors

    /**
     * Creates and returns a new ReductionBuilder that will use the provided
     * {@code extractor) to extract elements of type E.
     *
     * @param extractor (non-null)
     * @param <E>       element type
     * @return a new ReductionBuilder
     * @see LongReductionBuilder, DoubleReductionBuilder
     */
    static <E> ReductionBuilder<E> of(@NotNull final DocumentExtractor<E> extractor) {
        requireNonNull(extractor);
        return new ReductionUtil.VanillaReductionBuilder<>(extractor);
    }

    /**
     * Creates and returns a new LongReductionBuilder that will use the provided
     * {@code extractor) to extract elements of type {@code long}.
     *
     * @param extractor (non-null)
     * @return a new LongReductionBuilder
     * @see {@link #ofLong(ToLongDocumentExtractor)} and {@link #ofDouble(ToDoubleDocumentExtractor)}
     */
    static LongReductionBuilder ofLong(@NotNull final ToLongDocumentExtractor extractor) {
        requireNonNull(extractor);
        return new ReductionUtil.VanillaLongReductionBuilder(extractor);
    }

    /**
     * Creates and returns a new DoubleReductionBuilder that will use the provided
     * {@code extractor) to extract elements of type {@code double}.
     *
     * @param extractor (non-null)
     * @return a new DoubleReductionBuilder
     * @see {@link #of(DocumentExtractor)} and {@link #ofLong(ToLongDocumentExtractor)}
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