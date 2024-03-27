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

package net.openhft.chronicle.wire.internal.stream;

import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import net.openhft.chronicle.wire.domestic.extractor.ToDoubleDocumentExtractor;
import net.openhft.chronicle.wire.domestic.extractor.ToLongDocumentExtractor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * A utility class to provide additional functionality and support for streams.
 * This class is not meant to be instantiated.
 */
public final class StreamsUtil {

    private static final int BATCH_UNIT_INCREASE = 1 << 10; // Increment size for batch unit
    private static final int MAX_BATCH_SIZE = 1 << 24;     // Maximum size for batch processing

    // Suppresses default constructor, ensuring non-instantiability.
    private StreamsUtil() {
    }

    /**
     * The VanillaSpliterator class provides basic spliterator functionalities
     * over an iterator without any concurrent modifications.
     *
     * @param <T> the type of elements returned by this spliterator.
     */
    public static final class VanillaSpliterator<T> implements Spliterator<T> {

        private final Iterator<T> iterator; // The iterator this spliterator works on
        private int batchSize = 2 * BATCH_UNIT_INCREASE; // The current batch size for the spliterator

        /**
         * Constructs a VanillaSpliterator wrapping the provided iterator.
         *
         * @param iterator the iterator to be wrapped.
         */
        public VanillaSpliterator(@NotNull final Iterator<T> iterator) {
            requireNonNull(iterator);
            this.iterator = iterator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            synchronized (iterator) {
                if (iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                }
                return false;
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            // A bit problematic
            synchronized (iterator) {
                iterator.forEachRemaining(action);
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            synchronized (iterator) {
                if (iterator.hasNext()) {
                    final int n = Math.min(MAX_BATCH_SIZE, batchSize);
                    @SuppressWarnings("unchecked") final T[] a = (T[]) new Object[n];
                    int j = 0;
                    do {
                        a[j] = iterator.next();
                    } while (++j < n && iterator.hasNext());
                    batchSize += BATCH_UNIT_INCREASE;
                    return Spliterators.spliterator(a, 0, j, characteristics());
                }
                return null;
            }
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED + NONNULL;
        }
    }

    /**
     * The VanillaSpliteratorOfLong is a spliterator over a PrimitiveIterator.OfLong,
     * providing basic spliterator functionalities for long elements.
     */
    public static final class VanillaSpliteratorOfLong
            extends AbstractPrimitiveSpliterator<Long, LongConsumer, Spliterator.OfLong, PrimitiveIterator.OfLong>
            implements Spliterator.OfLong {

        /**
         * Constructs a VanillaSpliteratorOfLong wrapping the provided PrimitiveIterator.OfLong.
         *
         * @param iterator the iterator to be wrapped.
         */
        public VanillaSpliteratorOfLong(@NotNull final PrimitiveIterator.OfLong iterator) {
            super(iterator, (a, i) -> a.accept(i.nextLong()), PrimitiveIterator.OfLong::forEachRemaining);
        }

        /**
         * Attempts to split the iterator into a smaller spliterator of longs for parallel processing.
         *
         * @param n the size of the split.
         * @return a new Spliterator.OfLong covering the elements taken from this spliterator.
         */
        @NotNull
        protected OfLong split(int n) {
            final long[] a = new long[n];
            int j = 0;
            do {
                a[j] = iterator.nextLong();
            } while (++j < n && iterator.hasNext());
            return Spliterators.spliterator(a, 0, j, characteristics());
        }
    }

    /**
     * The VanillaSpliteratorOfDouble is a spliterator over a PrimitiveIterator.OfDouble,
     * providing basic spliterator functionalities for double elements.
     */
    public static final class VanillaSpliteratorOfDouble
            extends AbstractPrimitiveSpliterator<Double, DoubleConsumer, Spliterator.OfDouble, PrimitiveIterator.OfDouble>
            implements Spliterator.OfDouble {

        /**
         * Constructs a VanillaSpliteratorOfDouble wrapping the provided PrimitiveIterator.OfDouble.
         *
         * @param iterator the iterator to be wrapped.
         */
        public VanillaSpliteratorOfDouble(@NotNull final PrimitiveIterator.OfDouble iterator) {
            super(iterator, (a, i) -> a.accept(i.nextDouble()), PrimitiveIterator.OfDouble::forEachRemaining);
        }

        /**
         * Attempts to split the iterator into a smaller spliterator of doubles for parallel processing.
         *
         * @param n the size of the split.
         * @return a new Spliterator.OfDouble covering the elements taken from this spliterator.
         */
        @NotNull
        protected OfDouble split(int n) {
            final double[] a = new double[n];
            int j = 0;
            do {
                a[j] = iterator.nextDouble();
            } while (++j < n && iterator.hasNext());
            return Spliterators.spliterator(a, 0, j, characteristics());
        }
    }

    /**
     * Represents the abstract base class for primitive type spliterators. This class provides the foundational
     * mechanisms to iterate over primitive elements, supporting parallel processing by splitting the source into smaller chunks.
     *
     * <p>The abstract class expects the implementation of the {@code split} method, which dictates the splitting mechanism
     * for derived primitive spliterators.</p>
     *
     * @param <T> The type of the elements returned by this spliterator.
     * @param <C> The type of consumer for the primitive type.
     * @param <S> The type of spliterator that supports specialized primitive type operations.
     * @param <I> The type of primitive iterator.
     */
    abstract static class AbstractPrimitiveSpliterator<
            T,
            C,
            S extends Spliterator.OfPrimitive<T, C, S>,
            I extends PrimitiveIterator<T, C>
            >
            implements Spliterator.OfPrimitive<T, C, S> {

        // The primitive iterator responsible for iterating over elements of type T
        protected final I iterator;

        // A bi-consumer responsible for advancing the iterator given a specific action
        protected final BiConsumer<C, I> advancer;

        // A bi-consumer responsible for processing remaining elements from the iterator
        protected final BiConsumer<I, C> forEachRemainer;

        // Initial batch size used for splitting
        private int batchSize = 2 * BATCH_UNIT_INCREASE;

        /**
         * Constructs an AbstractPrimitiveSpliterator instance with the provided iterator, advancer, and forEachRemainer.
         *
         * @param iterator The iterator to be used by this spliterator.
         * @param advancer The advancer to dictate the advance mechanism of this iterator.
         * @param forEachRemainer The bi-consumer to process remaining elements from the iterator.
         */
        protected AbstractPrimitiveSpliterator(@NotNull final I iterator,
                                               @NotNull final BiConsumer<C, I> advancer,
                                               BiConsumer<I, C> forEachRemainer) {

            this.iterator = requireNonNull(iterator);
            this.advancer = requireNonNull(advancer);
            this.forEachRemainer = requireNonNull(forEachRemainer);
        }

        @Override
        public S trySplit() {
            synchronized (iterator) {
                if (iterator.hasNext()) {
                    final int n = Math.min(MAX_BATCH_SIZE, batchSize);
                    batchSize += BATCH_UNIT_INCREASE;
                    return split(n);
                }
                return null;
            }
        }

        /**
         * Abstract method to handle the actual splitting mechanism. Implementations should define how the
         * spliterator is split into smaller parts.
         *
         * @param n The suggested number of elements to include in the split.
         * @return A spliterator covering a portion of the elements.
         */
        @NotNull
        abstract S split(int n);

        @Override
        public boolean tryAdvance(C action) {
            synchronized (iterator) {
                if (iterator.hasNext()) {
                    advancer.accept(action, iterator);
                    return true;
                }
                return false;
            }
        }

        @Override
        public void forEachRemaining(C action) {
            // A bit problematic
            synchronized (iterator) {
                forEachRemainer.accept(iterator, action);
            }
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED;
        }
    }

    /**
     * Represents an iterator that extracts documents from a {@link MarshallableIn} stream using a
     * {@link DocumentExtractor}. This iterator traverses the underlying data source and uses
     * the extractor to transform each document into an instance of type {@code T}.
     *
     * @param <T> The type of object this iterator returns.
     */
    public static final class ExcerptIterator<T> implements Iterator<T> {

        // The data source from which documents are read
        private final MarshallableIn tailer;

        // The extractor used to transform the raw document into an object of type T
        private final DocumentExtractor<T> extractor;

        // The next item to be returned by this iterator
        private T next;

        /**
         * Constructs an instance of the ExcerptIterator with the provided tailer and extractor.
         *
         * @param tailer The data source from which documents will be read.
         * @param extractor The extractor used to transform the raw document into an object of type T.
         */
        public ExcerptIterator(@NotNull final MarshallableIn tailer,
                               @NotNull final DocumentExtractor<T> extractor) {
            this.tailer = tailer;
            this.extractor = extractor;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            long lastIndex = -1;
            for (; ; ) {
                try (final DocumentContext dc = tailer.readingDocument()) {
                    final Wire wire = dc.wire();
                    if (dc.isPresent() && wire != null) {
                        lastIndex = dc.index();
                        next = extractor.extract(wire, lastIndex);
                        if (next != null) {
                            return true;
                        }
                        // Retry reading yet another message
                    } else {
                        // We made no progress so we are at the end
                        break;
                    }
                }
            }
            return false;
        }

        @Override
        public T next() {
            if (next == null && !hasNext()) {
                throw new NoSuchElementException();
            }
            final T val = next;
            next = null;
            return val;
        }

    }

    /**
     * Represents an iterator that extracts documents containing long values from a {@link MarshallableIn}
     * stream using a {@link ToLongDocumentExtractor}. This iterator traverses the underlying data source
     * and uses the extractor to transform each document into a long value.
     */
    public static final class ExcerptIteratorOfLong implements PrimitiveIterator.OfLong {

        // The data source from which documents are read
        private final MarshallableIn tailer;

        // The extractor used to transform the raw document into a long value
        private final ToLongDocumentExtractor extractor;

        // Sentinel value indicating the absence of a next item. If 'next' equals this value,
        // then the next item is yet to be determined.
        private long next = Long.MIN_VALUE;

        /**
         * Constructs an instance of the ExcerptIteratorOfLong with the provided tailer and extractor.
         *
         * @param tailer The data source from which documents will be read.
         * @param extractor The extractor used to transform the raw document into a long value.
         */
        public ExcerptIteratorOfLong(@NotNull final MarshallableIn tailer,
                                     @NotNull final ToLongDocumentExtractor extractor) {
            this.tailer = tailer;
            this.extractor = extractor;
        }

        @Override
        public boolean hasNext() {
            if (next != Long.MIN_VALUE) {
                return true;
            }
            long lastIndex = -1;
            for (; ; ) {
                try (final DocumentContext dc = tailer.readingDocument()) {
                    final Wire wire = dc.wire();
                    if (dc.isPresent() && wire != null) {
                        lastIndex = dc.index();
                        next = extractor.extractAsLong(wire, lastIndex);
                        if (next != Long.MIN_VALUE) {
                            return true;
                        }
                        // Retry reading yet another message
                    } else {
                        // We made no progress so we are at the end
                        break;
                    }
                }
            }
            return false;
        }

        @Override
        public long nextLong() {
            if (next == Long.MIN_VALUE && !hasNext()) {
                throw new NoSuchElementException();
            }
            final long val = next;
            next = Long.MIN_VALUE;
            return val;
        }

    }

    /**
     * Represents an iterator that extracts documents containing double values from a {@link MarshallableIn}
     * stream using a {@link ToDoubleDocumentExtractor}. This iterator traverses the underlying data source
     * and uses the extractor to transform each document into a double value.
     */
    public static final class ExcerptIteratorOfDouble implements PrimitiveIterator.OfDouble {

        // The data source from which documents are read
        private final MarshallableIn tailer;

        // The extractor used to transform the raw document into a double value
        private final ToDoubleDocumentExtractor extractor;

        // Sentinel value indicating the absence of a next item. If 'next' equals this value,
        // then the next item is yet to be determined.
        private double next = Double.NaN;

        /**
         * Constructs an instance of the ExcerptIteratorOfDouble with the provided tailer and extractor.
         *
         * @param tailer The data source from which documents will be read.
         * @param extractor The extractor used to transform the raw document into a double value.
         */
        public ExcerptIteratorOfDouble(@NotNull final MarshallableIn tailer,
                                       @NotNull final ToDoubleDocumentExtractor extractor) {
            this.tailer = tailer;
            this.extractor = extractor;
        }

        @Override
        public boolean hasNext() {
            if (Double.isNaN(next)) {
                return true;
            }
            long lastIndex = -1;
            for (; ; ) {
                try (final DocumentContext dc = tailer.readingDocument()) {
                    final Wire wire = dc.wire();
                    if (dc.isPresent() && wire != null) {
                        lastIndex = dc.index();
                        next = extractor.extractAsDouble(wire, lastIndex);
                        if (!Double.isNaN(next)) {
                            return true;
                        }
                        // Retry reading yet another message
                    } else {
                        // We made no progress so we are at the end
                        break;
                    }
                }
            }
            return false;
        }

        @Override
        public double nextDouble() {
            if (Double.isNaN(next) && !hasNext()) {
                throw new NoSuchElementException();
            }
            final double val = next;
            next = Double.NaN;
            return val;
        }

    }

}
