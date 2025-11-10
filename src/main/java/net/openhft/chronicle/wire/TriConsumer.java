//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents an operation that accepts three input arguments and returns no result.
 * This is a three-arity specialization of {@code Consumer} and functions much
 * like {@link java.util.function.BiConsumer} but with an additional argument.
 * Unlike most other functional interfaces, {@code TriConsumer} is expected to operate
 * via side effects. This interface is useful in scenarios where lambda expressions
 * or method references would benefit from custom manipulations of three separate arguments.
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <V> the type of the third argument to the operation
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t The first input argument.
     * @param u The second input argument.
     * @param v The third input argument.
     * @throws InvalidMarshallableException if there are issues with marshalling
     *                                      during the accept operation.
     */
    void accept(T t, U u, V v) throws InvalidMarshallableException;

    /**
     * Returns a composed {@code TriConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after The {@code TriConsumer} to execute after this one completes.
     * @return A new composed {@code TriConsumer} that performs this operation
     * followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    @NotNull
    default TriConsumer<T, U, V> andThen(@NotNull TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);

        return (t, u, v) -> {
            accept(t, u, v);
            after.accept(t, u, v);
        };
    }
}
