/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Represents a consumer that accepts two objects of types T and V, and an integer value.
 * This functional interface provides a mechanism to perform an action with three inputs,
 * combining object-object-int in the provided order. It's designed for scenarios
 * where two objects and an integer are needed for processing or computation.
 *
 * <p>Example Usage:
 * <pre>
 *     ObjectIntObjectConsumer&lt;String, Double&gt; printer = (str, num, dbl) -&gt;
 *         System.out.println(str + " - " + num + " - " + dbl);
 *     printer.accept("Value", 5, 20.5);
 * </pre>
 * The above will print: Value - 5 - 20.5
 *
 * @param <T> The type of the first object to be consumed.
 * @param <V> The type of the second object to be consumed.
 */
@FunctionalInterface
public interface ObjectIntObjectConsumer<T, V> {

    /**
     * Performs the operation defined by this consumer.
     *
     * @param t the first input argument of type {@code T}
     * @param u the second input argument, an {@code int} value
     * @param v the third input argument of type {@code V}
     */
    void accept(T t, int u, V v);
}
