/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy for serialising and deserialising objects of type {@code T}.
 * Implementations read data from a {@link ValueIn}, create instances as
 * required and describe the wire format used. A strategy usually targets a
 * particular Java type or category such as enums, {@link Marshallable}
 * objects or collections. It dictates how objects are converted to and from a
 * wire representation. See {@link SerializationStrategies} for the common
 * built-in strategies.
 */
public interface SerializationStrategy {

    /**
     * Reads an object of type {@code T} from the provided input source and populates
     * the given 'using' object, if not null. The method uses the given {@link BracketType}
     * to aid in the deserialization.
     *
     * @param clazz       expected class of the object, or {@code null} to infer
     *                    from the wire or {@code using} instance
     * @param using       An optional object of type {@code T} that can be populated with the read data.
     *      *              If null, a new object will be created or an exception might be thrown depending on implementation.
     * @param in          source of the wire data
     * @param bracketType hint about the expected structure such as map, sequence
     *                    or none
     * @return the populated or newly created object, or {@code null} if no
     * instance could be obtained
     * @throws InvalidMarshallableException if the deserialisation fails
     * @param <T> concrete object type being deserialised
     */
    @Nullable
    <T> T readUsing(Class<?> clazz, T using, ValueIn in, BracketType bracketType) throws InvalidMarshallableException;

    /**
     * Constructs and returns a new instance of the provided {@code type}
     * as a reference. If the instance cannot be constructed for any reason,
     * {@code null} is returned.
     *
     * @param type The class type for which a new instance is required.
     * @param <T>  concrete object type to instantiate
     * @return A new instance of the provided {@code type} or {@code null} if instantiation is not possible.
     */
    @Nullable
    <T> T newInstanceOrNull(Class<T> type);

    /**
     * Returns the primary Java {@link Class} that this strategy deals with.
     * It may be a concrete type, an interface or a broad category such as
     * {@code java.lang.Enum}.
     *
     * @return handled class type
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
