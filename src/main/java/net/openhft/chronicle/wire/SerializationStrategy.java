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
 * <p>
 * Each implementation typically targets a particular Java type (for example
 * {@link Enum enums}, {@link java.util.List lists} or {@link Marshallable} objects)
 * and defines how instances of that type are converted to and from a wire
 * representation.  See {@link SerializationStrategies} for the common built-in
 * strategies.
 */
public interface SerializationStrategy {

    /**
     * Reads an object of type {@code T} from the provided input source and populates
     * the given {@code using} instance if one is supplied.  The {@code clazz}
     * parameter describes the expected type.  It may be a super type of the
     * actual object being read if the concrete type can be inferred from the
     * wire data.  If {@code using} is {@code null}, the implementation may call
     * {@link #newInstanceOrNull(Class)} to obtain a new instance.  The provided
     * {@link BracketType} hints at the structural representation (map, sequence
     * or none) of the value being read.
     *
     * @param clazz       the expected class of the object to read.  May be {@code null}
     *                    if the type is to be inferred by the strategy.
     * @param using       an optional existing instance to populate.  If {@code null}
     *                    the strategy may create a new instance.
     * @param in The input source containing serialized data.
     * @param bracketType The type of bracket used in the serialized format.
     * @return The populated or newly created object of type {@code T}.
     * @throws InvalidMarshallableException If an error occurs during the deserialization process.
     */
    @Nullable
    <T> T readUsing(Class<?> clazz, T using, ValueIn in, BracketType bracketType) throws InvalidMarshallableException;

    /**
     * Constructs and returns a new instance of the provided {@code type}.  This
     * method is used by the deserialization process when no existing instance is
     * supplied via the {@code using} parameter of {@link #readUsing}.  If a new
     * instance cannot be constructed (for example the type is an interface or
     * abstract class) {@code null} is returned.
     *
     * @param type The class for which a new instance is required.
     * @return a new instance or {@code null} if instantiation is not possible.
     */
    @Nullable
    <T> T newInstanceOrNull(Class<T> type);

    /**
     * Returns the primary Java {@link Class} that this strategy serializes and
     * deserializes.  This may be a concrete class, an interface or a more
     * generic type such as {@link Object}.
     */
    Class<?> type();

    /**
     * Returns the {@link BracketType} associated with this strategy.  This
     * describes the structural form expected when reading or writing a value –
     * for example {@link BracketType#MAP} for objects, {@link BracketType#SEQ}
     * for collections or {@link BracketType#NONE} for scalar values.
     */
    @NotNull
    BracketType bracketType();
}
