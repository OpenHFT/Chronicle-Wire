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

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link Marshallable} that exposes a portion of its state as a key.
 * Useful for map-like collections where objects are identified by part of their
 * content.
 */
public interface KeyedMarshallable {

    /**
     * Writes the key portion of this instance to the supplied bytes.
     * The default implementation delegates to {@link Wires#writeKey(Object, Bytes)}.
     *
     * @param bytes destination buffer for the key
     */
    @SuppressWarnings("rawtypes")
    default void writeKey(@NotNull Bytes<?> bytes) {
        Wires.writeKey(this, bytes);
    }
}
