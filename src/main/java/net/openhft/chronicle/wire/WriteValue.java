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
 * Represents an entity capable of writing its value to a specified output format.
 * This interface is intended to be implemented by classes or lambdas that need
 * to serialize their state to a given value representation.
 * <p>
 * It's designed with the {@code @FunctionalInterface} annotation, suggesting that
 * it's primarily intended for lambda expressions and method references.
 * The {@code @DontChain} annotation indicates a recommendation against chaining
 * methods for implementations of this interface.
 */
@FunctionalInterface
@DontChain
public interface WriteValue {

    /**
     * Writes the current state of the implementing object as a value
     * to the provided {@link ValueOut} representation.
     *
     * @param out The output representation to write the value to.
     * @throws InvalidMarshallableException if any serialization error occurs.
     */
    void writeValue(ValueOut out) throws InvalidMarshallableException;
}
