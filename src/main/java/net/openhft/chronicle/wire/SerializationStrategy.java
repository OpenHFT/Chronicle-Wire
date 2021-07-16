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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SerializationStrategy<T> {

    @Nullable
    T readUsing(T using, ValueIn in, BracketType bracketType);

    /**
     * Constructs and returns a new instance using the provided {@code type}
     * as a reference. If the instance cannot be constructed, {@code null}
     * is returned.
     *
     * @return a new instance of the provided {@code type} or {@code null}
     */
    @Nullable
    T newInstanceOrNull(Class<T> type);

    /**
     * Returns the {@code type} handled by this serialization strategy.
     *
     * @return the {@code type} handled by this serialization strategy.
     */
    Class<T> type();

    /**
     * Returns the {@link BracketType} used by this serialization strategy.
     *
     * @return the {@link BracketType} used by this serialization strategy.
     */
    @NotNull
    BracketType bracketType();

    default T readResolve(T t) {
        return t;
    }
}