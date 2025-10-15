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

/**
 * Parses fields identified by a numeric ID.
 * This is primarily used with binary wire formats where fields can be
 * identified by numeric IDs instead of string names, for compactness and
 * efficiency. It is typically registered with a {@link WireParser}
 * (e.g., {@link VanillaWireParser}) as a fallback or for specific
 * field numbers.
 */
public interface FieldNumberParselet {

    /**
     * Invoked when a numeric field ID is parsed.
     *
     * @param methodId the numeric field ID read from the binary wire
     * @param wire     the {@link WireIn} from which the value should be read;
     *                 use {@code wire.getValueIn()} to access and deserialise the value
     * @throws InvalidMarshallableException if the value cannot be processed
     */
    void readOne(long methodId, WireIn wire) throws InvalidMarshallableException;
}
