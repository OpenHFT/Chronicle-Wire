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
 * An interface that represents a marshallable object that can write itself to a {@link WireOut} object.
 * This interface extends both {@link WriteValue} and {@link CommonMarshallable}.
 */
@FunctionalInterface
@DontChain
public interface WriteMarshallable extends WriteValue, CommonMarshallable {

    /**
     * An empty instance of WriteMarshallable that performs no action.
     */
    WriteMarshallable EMPTY = wire -> {
        // nothing
    };

    /**
     * Writes this object to the specified wire.
     *
     * @param wire the wire to write to.
     * @throws InvalidMarshallableException if an error occurs during marshalling of this object
     */
    void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException;

    /**
     * Default implementation that writes this object as a marshallable.
     *
     * @param out the ValueOut object to write to
     * @throws InvalidMarshallableException if an error occurs during marshalling of this object
     */
    @Override
    default void writeValue(@NotNull ValueOut out) throws InvalidMarshallableException {
        out.marshallable(this);
    }

    /**
     * Provides the size in bytes to assume the length of the binary data will be.
     *
     * @return the size in bytes to assume the length will be, default is 32 bits.
     */
    default BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_32BIT;
    }
}
