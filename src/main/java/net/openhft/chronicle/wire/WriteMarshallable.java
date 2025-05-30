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
 * Represents an entity that can write its state to a wire.  It is a
 * {@code @FunctionalInterface} and is commonly used by DTOs or other
 * objects that need lightweight serialisation.  The interface extends
 * {@link WriteValue} and {@link CommonMarshallable}.
 */
@FunctionalInterface
@DontChain
public interface WriteMarshallable extends WriteValue, CommonMarshallable {

    /**
     * A no-operation {@code WriteMarshallable} that writes nothing to the
     * wire.  Useful as a placeholder.
     */
    WriteMarshallable EMPTY = wire -> {
        // nothing
    };

    /**
     * Write this object's state to the supplied wire.
     * Implementations should read their fields and serialise them to the
     * given output.
     *
     * @param wire the wire to write to
     * @throws InvalidMarshallableException if serialization fails
     */
    void writeMarshallable(@NotNull WireOut wire)
            throws InvalidMarshallableException;

    /**
     * Serialises this object as a value.  The default implementation
     * simply delegates to {@code out.marshallable(this)}.
     *
     * @param out the output to write to
     * @throws InvalidMarshallableException if any error occurs
     */
    @Override
    default void writeValue(@NotNull ValueOut out)
            throws InvalidMarshallableException {
        out.marshallable(this);
    }

    /**
     * Returns the length prefix to use when serialising this object.  It is
     * mainly relevant for binary wire formats that prefix marshallable
     * objects with their length.  See {@link BinaryLengthLength} for details.
     *
     * @return the assumed length prefix
     */
    default BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_32BIT;
    }
}
