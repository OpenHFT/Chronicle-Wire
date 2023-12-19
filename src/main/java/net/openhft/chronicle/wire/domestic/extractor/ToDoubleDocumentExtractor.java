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

import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;

import static java.lang.Double.isNaN;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

@FunctionalInterface
public interface ToDoubleDocumentExtractor {

    /**
     * Extracts a value of type {@code double } from the provided {@code wire} and {@code index} or else {@link Double#NaN}
     * if no value can be extracted.
     * <p>
     * {@link Double#NaN} may be returned if the queue was written with a method writer and there are messages in the
     * queue but of another type.
     * <p>
     * Extractors that must include {@link Double#NaN} as a valid value must use other means of
     * aggregating values (e.g. use an {@link DocumentExtractor DocumentExtractor<Double> }.
     *
     * @param wire  to use
     * @param index to use
     * @return extracted value or {@link Double#NaN}
     */
    double extractAsDouble(@NotNull Wire wire, @NonNegative long index);

    /**
     * Creates and returns a new ToDoubleDocumentExtractor consisting of the results (of type R) of applying the provided
     * {@code mapper } to the elements of this ToDoubleDocumentExtractor.
     * <p>
     * Values mapped to {@link Double#NaN} are removed.
     *
     * @param mapper to apply
     * @return a new mapped ToDoubleDocumentExtractor
     * @throws NullPointerException if the provided {@code mapper} is {@code null}
     */
    default ToDoubleDocumentExtractor map(@NotNull final DoubleUnaryOperator mapper) {
        requireNonNull(mapper);
        return (wire, index) -> {
            final double value = extractAsDouble(wire, index);
            if (isNaN(value)) {
                return Double.NaN;
            }
            return mapper.applyAsDouble(value);
        };
    }

    /**
     * Creates and returns a new DocumentExtractor consisting of applying the provided
     * {@code mapper } to the elements of this DocumentExtractor.
     * <p>
     * Values mapped to {@link Double#NaN } are removed.
     *
     * @param mapper to apply
     * @return a new mapped DocumentExtractor
     * @throws NullPointerException if the provided {@code mapper} is {@code null}
     */
    default <T> DocumentExtractor<T> mapToObj(@NotNull final DoubleFunction<? extends T> mapper) {
        requireNonNull(mapper);
        return (wire, index) -> {
            final double value = extractAsDouble(wire, index);
            if (isNaN(value)) {
                return null;
            }
            return mapper.apply(value);
        };
    }

    /**
     * Creates and returns a new ToLongDocumentExtractor consisting of applying the provided
     * {@code mapper } to the elements of this ToDoubleDocumentExtractor.
     * <p>
     * Values mapped to {@link Double#NaN } are removed.
     *
     * @param mapper to apply
     * @return a new mapped ToLongDocumentExtractor
     * @throws NullPointerException if the provided {@code mapper} is {@code null}
     */
    default ToLongDocumentExtractor mapToLong(@NotNull final DoubleToLongFunction mapper) {
        requireNonNull(mapper);
        return (wire, index) -> {
            final double value = extractAsDouble(wire, index);
            if (isNaN(value)) {
                return Long.MIN_VALUE;
            }
            return mapper.applyAsLong(value);
        };
    }

    /**
     * Creates and returns a new ToDoubleDocumentExtractor consisting of the elements of this ToDoubleDocumentExtractor
     * that match the provided {@code predicate}.
     *
     * @param predicate to apply to each element to determine if it
     *                  should be included
     * @return a ToDoubleDocumentExtractor consisting of the elements of this ToDoubleDocumentExtractor that match
     * @throws NullPointerException if the provided {@code predicate} is {@code null}
     */
    default ToDoubleDocumentExtractor filter(@NotNull final DoublePredicate predicate) {
        requireNonNull(predicate);
        return (wire, index) -> {
            final double value = extractAsDouble(wire, index);
            if (isNaN(value)) {
                // The value is already filtered so just propagate the lack of a value
                return Double.NaN;
            }
            return predicate.test(value)
                    ? value
                    : Double.NaN;
        };
    }

    // skip

    // peek

}
