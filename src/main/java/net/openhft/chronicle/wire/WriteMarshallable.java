/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
@DontChain
public interface WriteMarshallable extends WriteValue, CommonMarshallable {
    WriteMarshallable EMPTY = wire -> {
        // nothing
    };

    /**
     * Write data to the wire
     *
     * @param wire to write to.
     */
    void writeMarshallable(@NotNull WireOut wire);

    @Override
    default void writeValue(@NotNull ValueOut out) {
        out.marshallable(this);
    }

    /**
     * @return the size in bytes to assume the length will be
     */
    default BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_32BIT;
    }
}
