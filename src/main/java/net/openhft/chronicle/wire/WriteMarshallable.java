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

import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a marshallable entity capable of writing its state to a given wire format.
 * Implementations of this interface can describe their serialization logic by defining
 * the {@link #writeMarshallable(WireOut)} method.
 * <p>
 * This interface is annotated with {@code @FunctionalInterface}, indicating that it is
 * intended to be used primarily for lambda expressions and method references.
 * Furthermore, the {@code @DontChain} annotation suggests that implementations should
 * not be chained for certain operations.
 */
@FunctionalInterface
@DontChain
public interface WriteMarshallable extends WriteValue, CommonMarshallable {

    /**
     * Represents an empty marshallable entity that performs no actions
     * when its {@code writeMarshallable} method is invoked.
     */
    WriteMarshallable EMPTY = wire -> {
        // nothing
    };

    /**
     * Write the current state of the marshallable entity to the provided wire.
     *
     * @param wire The wire format to write to.
     * @throws InvalidMarshallableException if any serialization error occurs.
     */
    void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException;

    /**
     * Writes the current state of the marshallable entity as a value
     * to the provided output.
     *
     * @param out The output to write to.
     * @throws InvalidMarshallableException if any serialization error occurs.
     */
    @Override
    default void writeValue(@NotNull ValueOut out) throws InvalidMarshallableException {
        out.marshallable(this);
    }

    /**
     * Provides an assumed length in bytes for the serialized form of this entity.
     * This is useful for pre-allocating resources or optimizing serialization
     * and deserialization processes.
     *
     * @return The binary length length indicating the size in bytes.
     */
    default BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_32BIT;
    }
}
