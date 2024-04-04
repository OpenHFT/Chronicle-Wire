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

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a strategy for serializing and deserializing objects of type {@code T}.
 * Implementations of this interface define methods for reading, instantiating,
 * and providing metadata about the serialized format.
 *
 * @param <T> the type of objects this strategy can handle.
 */
public interface SerializationStrategy<T> {

    /**
     * Reads an object of type {@code T} from the provided input source and populates
     * the given 'using' object, if not null. The method uses the given {@link BracketType}
     * to aid in the deserialization.
     *
     * @param clazz The class type to be deserialized.
     * @param using An optional object of type {@code T} that can be populated with the read data.
     *              If null, a new object will be created or an exception might be thrown depending on implementation.
     * @param in The input source containing serialized data.
     * @param bracketType The type of bracket used in the serialized format.
     * @return The populated or newly created object of type {@code T}.
     * @throws InvalidMarshallableException If an error occurs during the deserialization process.
     */
    @Nullable
    <T> T readUsing(Class<?> clazz, T using, ValueIn in, BracketType bracketType) throws InvalidMarshallableException;

    /**
     * Constructs and returns a new instance of the provided {@code type}
     * as a reference. If the instance cannot be constructed for any reason,
     * {@code null} is returned.
     *
     * @param type The class type for which a new instance is required.
     * @return A new instance of the provided {@code type} or {@code null} if instantiation is not possible.
     */
    @Nullable
    <T> T newInstanceOrNull(Class<T> type);

    /**
     * Returns the class type of objects this serialization strategy is designed to handle.
     *
     * @return The class type of objects this strategy can serialize and deserialize.
     */
    Class<?> type();

    /**
     * Provides the bracket type used in the serialized format, which might
     * give hints or constraints on how the data is structured.
     *
     * @return the {@link BracketType} used by this serialization strategy.
     */
    @NotNull
    BracketType bracketType();
}
