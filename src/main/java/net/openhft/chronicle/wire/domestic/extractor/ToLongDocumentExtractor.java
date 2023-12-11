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

package net.openhft.chronicle.wire.domestic.extractor;

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongUnaryOperator;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

@FunctionalInterface
public interface ToLongDocumentExtractor {

    /**
     * Extracts a value of type {@code long } from the provided {@code wire} and {@code index} or else {@link Long#MIN_VALUE}
     * if no value can be extracted.
     * <p>
     * {@link Long#MIN_VALUE} may be returned if the queue was written with a method writer and there are messages in the
     * queue but of another type.
     * <p>
     * Extractors that must include {@link Long#MIN_VALUE} as a valid value must use other means of
     * aggregating values (e.g. use an {@link DocumentExtractor DocumentExtractor<Long> }.
     *
     * @param wire  to use
     * @param index to use
     * @return extracted value or {@link Long#MIN_VALUE}
     */
    long extractAsLong(@NotNull Wire wire, @NonNegative long index);

    /**
     * Creates and returns a new ToLongDocumentExtractor consisting of the results (of type R) of applying the provided
     * {@code mapper } to the elements of this ToLongDocumentExtractor.
     * <p>
     * Values mapped to {@link Long#MIN_VALUE} are removed.
     *
     * @param mapper to apply
     * @return a new mapped ToLongDocumentExtractor
     * @throws NullPointerException if the provided {@code mapper} is {@code null}
     */
    default ToLongDocumentExtractor map(@NotNull final LongUnaryOperator mapper) {
        requireNonNull(mapper);
        return (wire, index) -> {
            final long value = extractAsLong(wire, index);
            if (value == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            return mapper.applyAsLong(value);
        };
    }

    /**
     * Creates and returns a new DocumentExtractor consisting of applying the provided
     * {@code mapper } to the elements of this ToLongDocumentExtractor.
     * <p>
     * Values mapped to {@link Long#MIN_VALUE } are removed.
     *
     * @param mapper to apply
     * @return a new mapped DocumentExtractor
     * @throws NullPointerException if the provided {@code mapper} is {@code null}
     */
    default <T> DocumentExtractor<T> mapToObj(@NotNull final LongFunction<? extends T> mapper) {
        requireNonNull(mapper);
        return (wire, index) -> {
            final long value = extractAsLong(wire, index);
            if (value == Long.MIN_VALUE) {
                return null;
            }
            return mapper.apply(value);
        };
    }

    /**
     * Creates and returns a new ToDoubleDocumentExtractor consisting of applying the provided
     * {@code mapper } to the elements of this ToLongDocumentExtractor.
     * <p>
     * Values mapped to {@link Long#MIN_VALUE } are removed.
     *
     * @param mapper to apply
     * @return a new mapped ToDoubleDocumentExtractor
     * @throws NullPointerException if the provided {@code mapper} is {@code null}
     */
    default ToDoubleDocumentExtractor mapToDouble(@NotNull final LongToDoubleFunction mapper) {
        requireNonNull(mapper);
        return (wire, index) -> {
            final long value = extractAsLong(wire, index);
            if (value == Long.MIN_VALUE) {
                return Double.NaN;
            }
            return mapper.applyAsDouble(value);
        };
    }

    /**
     * Creates and returns a new ToLongDocumentExtractor consisting of the elements of this ToLongDocumentExtractor
     * that match the provided {@code predicate}.
     *
     * @param predicate to apply to each element to determine if it
     *                  should be included
     * @return a ToLongDocumentExtractor consisting of the elements of this ToLongDocumentExtractor that match
     * @throws NullPointerException if the provided {@code predicate} is {@code null}
     */
    default ToLongDocumentExtractor filter(@NotNull final LongPredicate predicate) {
        requireNonNull(predicate);
        return (wire, index) -> {
            final long value = extractAsLong(wire, index);
            if (value == Long.MIN_VALUE) {
                // The value is already filtered so just propagate the lack of a value
                return Long.MIN_VALUE;
            }
            return predicate.test(value)
                    ? value
                    : Long.MIN_VALUE;

        };
    }

    // skip

    // peek

    static ToLongDocumentExtractor extractingIndex() {
        return (wire, index) -> index;
    }

}
