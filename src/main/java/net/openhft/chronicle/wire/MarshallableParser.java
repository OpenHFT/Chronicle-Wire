/*
 * Copyright 2016-2025 chronicle.software
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

/**
 * Functional interface describing a strategy for deserialising data.
 * <p>
 * Implementations convert a {@link ValueIn} representation to an object of type {@code T}.
 * This allows custom logic to be supplied wherever a {@code ValueIn} needs to be turned into a concrete object.
 *
 * @param <T> the type produced after parsing the input value
 */
@FunctionalInterface
public interface MarshallableParser<T> {

    /**
     * Parses the provided {@code ValueIn} into an instance of type {@code T}.
     *
     * @param valueIn the {@link ValueIn} holding the serialised form
     * @return non-null instance of {@code T} deserialised from {@code valueIn}
     */
    @NotNull
    T parse(ValueIn valueIn);
}
