/*
 * Copyright 2016-2025 chronicle.software
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
 * Functional interface representing a consumer that accepts two objects and an
 * integer. It is useful when a lambda or method reference needs to process an
 * object-int-object tuple in that order.
 *
 * <p>Example usage:
 * <pre>
 *     ObjectIntObjectConsumer&lt;String, Double&gt; printer = (str, num, dbl) -&gt;
 *         System.out.println(str + " - " + num + " - " + dbl);
 *     printer.accept("Value", 5, 20.5);
 * </pre>
 * The example prints: Value - 5 - 20.5
 *
 * @param <T> type of the first object argument
 * @param <V> type of the second object argument
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
