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

import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for objects that can write their state to a
 * {@link WireOut}.  Often implemented by DTOs or other entities requiring
 * serialisation.  It also extends {@link WriteValue} and
 * {@link CommonMarshallable}.
 */
@FunctionalInterface
@DontChain
public interface WriteMarshallable extends WriteValue, CommonMarshallable {

    /**
     * Placeholder that writes nothing when invoked.
     */
    WriteMarshallable EMPTY = wire -> {
        // nothing
    };

    /**
     * Write this object's state to {@code wire}.
     */
    void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException;

    /**
     * Default implementation delegates to {@code out.marshallable(this)}.
     */
    @Override
    default void writeValue(@NotNull ValueOut out) throws InvalidMarshallableException {
        out.marshallable(this);
    }

    /**
     * Hint for binary wire formats about the length prefix to use.
     */
    default BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_32BIT;
    }
}
