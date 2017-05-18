/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface WriteMarshallable extends WriteValue {
    WriteMarshallable EMPTY = wire -> {
        // nothing
    };

    /**
     * Write data to the wire
     *
     * @param wire to write to.
     */
    void writeMarshallable(@NotNull WireOut wire);

    default void writeValue(@NotNull ValueOut out) {
        out.marshallable(this);
    }
}
