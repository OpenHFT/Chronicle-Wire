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
 * Functional interface representing a marshaller that populates an instance of type {@code T}
 * with data read from a {@code WireIn} source. This interface allows decoupling the
 * deserialization logic from the object's representation, which can be especially useful
 * for reusing an existing instance rather than creating a new object every time data is read.
 *
 * <p>
 * Example usage might involve reading an object's state from a network stream or file and
 * updating the existing object's fields based on the stream content.
 * </p>
 *
 * @param <T> The type of object being read and populated.
 * @since 2023-09-11
 */
@FunctionalInterface
public interface ReadingMarshaller<T> {

    /**
     * Reads data from the provided {@code WireIn} and updates the state of the given object
     * of type {@code T}. Implementations of this method should ensure that the state of
     * the object {@code t} reflects the content of the wire input.
     *
     * @param t  The object of type {@code T} whose state should be updated based on the wire content.
     * @param in The wire input source from which data should be read.
     */
    void readFromWire(T t, WireIn in);
}
