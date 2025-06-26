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
 * Functional interface invoked when a field name is parsed.
 * It is typically registered with a {@link WireParser} to define how to
 * deserialise the value associated with a specific field name.
 */
@FunctionalInterface
public interface WireParselet {

    /**
     * Invoked with the field name and corresponding value.
     *
     * @param s   the field name that matched this parselet
     * @param in  the {@link ValueIn} positioned at the value for {@code s}. Use it to
     *            deserialise the value
     * @throws InvalidMarshallableException if the value cannot be processed
     */
    void accept(CharSequence s, ValueIn in) throws InvalidMarshallableException;
}
