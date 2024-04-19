/*
 * Copyright 2016-2020 chronicle.software
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
     * @param t The first object argument.
     * @param u The integer argument.
     * @param v The second object argument.
     */
    void accept(T t, int u, V v);
}
