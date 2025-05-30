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
 * Functional interface for objects that can write their state to a
 * {@link WireOut}.  Often used by DTOs or other objects that need to be
 * serialized.
 */
@FunctionalInterface
@DontChain
public interface WriteMarshallable extends WriteValue, CommonMarshallable {

    /**
     * A no-op instance that writes nothing.  Useful as a placeholder
     * implementation of {@code WriteMarshallable}.
     */
    WriteMarshallable EMPTY = wire -> {
        // nothing
    };

    /**
     * Write this object's state to {@code wire}.  Implementations should read
     * their own fields and emit them via the provided {@link WireOut}.
     */
    void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException;

    /**
     * Default implementation that simply calls {@code out.marshallable(this)}.
     */
    @Override
    default void writeValue(@NotNull ValueOut out) throws InvalidMarshallableException {
        out.marshallable(this);
    }

    /**
     * Hint about the length prefix used when this object is written in binary
     * form.  Defaults to {@link BinaryLengthLength#LENGTH_32BIT}.
     */
    default BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_32BIT;
    }
}
