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

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.InvalidMarshallableException;

/**
 * A functional interface representing a writer that writes a value to a {@link ValueOut} object.
 * It is annotated with {@link DontChain} to prevent method chaining on its implementation.
 */
@FunctionalInterface
@DontChain
public interface WriteValue {

    /**
     * Writes a value to the provided {@link ValueOut}.
     *
     * @param out The ValueOut object to write to
     * @throws InvalidMarshallableException if an error occurs during marshalling of the value
     */
    void writeValue(ValueOut out) throws InvalidMarshallableException;
}
