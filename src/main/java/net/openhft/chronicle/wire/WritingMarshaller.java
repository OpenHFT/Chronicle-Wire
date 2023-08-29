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
 * Functional interface that represents a marshaller responsible for
 * serializing objects of type {@code T} to a wire format.
 * Implementors of this interface provide custom serialization mechanisms
 * for instances of type {@code T} to be written to the specified output format.
 *
 * Designed with the {@code @FunctionalInterface} annotation, it suggests that
 * the primary purpose is for lambda expressions or method references that define
 * custom serialization behavior for specific types.
 *
 * @param <T> the type of the object to be serialized to the wire format.
 * @since 2023-08-29
 */
@FunctionalInterface
public interface WritingMarshaller<T> {

    /**
     * Serializes the specified object of type {@code T} to the provided {@link WireOut} format.
     *
     * @param t The object of type {@code T} to be serialized.
     * @param out The wire output representation to which the object should be written.
     */
    void writeToWire(T t, WireOut out);
}
