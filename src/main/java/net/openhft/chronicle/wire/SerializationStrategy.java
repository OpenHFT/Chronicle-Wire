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
     * Reads an object of type {@code T} from the supplied input.
     *
     * @param clazz       expected class of the object, or {@code null} to infer
     *                    from the wire or {@code using} instance
     * @param using       optional instance to populate; if {@code null} this
     *                    method may call {@link #newInstanceOrNull(Class)}. If
     *                    that also returns {@code null} the strategy decides
     *                    whether to return {@code null} or throw
     *                    {@link InvalidMarshallableException}
     * @param in          source of the wire data
     * @param bracketType hint about the expected structure such as map, sequence
     *                    or none
     * @return the populated or newly created object, or {@code null} if no
     * instance could be obtained
     * @throws InvalidMarshallableException if the deserialisation fails
     */
    @Nullable
    <T> T readUsing(Class<?> clazz, T using, ValueIn in, BracketType bracketType) throws InvalidMarshallableException;

    /**
     * Attempts to create a new instance of the given type.
     * Called when {@link #readUsing} needs an object but none was supplied.
     *
     * @param type class of object to be created
     * @return newly created instance or {@code null} if construction failed
     */
    @Nullable
    <T> T newInstanceOrNull(Class<T> type);

    /**
     * Returns the primary Java {@link Class} that this strategy deals with.
     * It may be a concrete type, an interface or a broad category such as
     * {@code java.lang.Enum}.
     */
    Class<?> type();

    /**
     * The bracket type expected in the wire representation.
     * For example {@link BracketType#MAP} for key-value pairs,
     * {@link BracketType#SEQ} for sequences or {@link BracketType#NONE} for
     * scalar values.
     *
     * @return the {@link BracketType} used by this strategy
     */
    @NotNull
    BracketType bracketType();
}
