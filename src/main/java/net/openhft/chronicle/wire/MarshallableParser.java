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

import org.jetbrains.annotations.NotNull;

/**
 * This is the {@code MarshallableParser} functional interface.
 * It provides a contract for parsing methods that convert a given {@link ValueIn} into an instance of type {@code T}.
 * Designed to be used in scenarios where marshalling is required to transform input values into desired data types.
 *
 * @param <T> the type of the object that will be produced after parsing the input value.
 */
@FunctionalInterface
public interface MarshallableParser<T> {

    /**
     * Parses the provided {@code ValueIn} into an instance of type {@code T}.
     *
     * @param valueIn the input value to be parsed.
     * @return the parsed instance of type {@code T}.
     */
    @NotNull
    T parse(ValueIn valueIn);
}
