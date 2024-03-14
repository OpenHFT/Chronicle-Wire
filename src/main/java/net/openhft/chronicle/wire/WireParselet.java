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

import net.openhft.chronicle.core.io.InvalidMarshallableException;

/**
 * Represents a functional interface that can process wire input based on a given
 * character sequence key and a value input. The `WireParselet` can be seen as a
 * specific action or handler for a particular key found in the wire input.
 */
@FunctionalInterface
public interface WireParselet {

    /**
     * Consumes and processes a wire input based on a given character sequence
     * and a value input.
     *
     * @param s The character sequence (usually representing a key) from the wire input.
     * @param in The value associated with the key in the wire input.
     * @throws InvalidMarshallableException If there's an issue with processing the data.
     */
    void accept(CharSequence s, ValueIn in) throws InvalidMarshallableException;
}
