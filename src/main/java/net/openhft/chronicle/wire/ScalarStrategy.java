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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementation of {@link SerializationStrategy} for simple scalar values.  It
 * is typically used for primitive wrappers or other types that appear in the
 * wire as single values without any surrounding structure
 * ({@link BracketType#NONE}).
 *
 * @param <E> The type of the scalar value that this strategy handles.
 */
class ScalarStrategy<E> implements SerializationStrategy {
    /** Function used to read the value from a {@link ValueIn}. */
    final BiFunction<? super E, ValueIn, E> read;
    /** The scalar value type. */
    private final Class<E> type;

    /**
     * Constructs a new {@code ScalarStrategy} with the given type and read function.
     *
     * @param type The class type of the scalar value.
     * @param read The function used to read the scalar value.
     */
    ScalarStrategy(Class<E> type, @NotNull BiFunction<? super E, ValueIn, E> read) {
        this.type = type;
        this.read = read;
    }

    /**
     * Factory method to create a new instance of {@code ScalarStrategy}
     * with the provided class type and read function.
     *
     * @param clazz The class type of the scalar value.
     * @param read  The function used to read the scalar value.
     * @param <E>   The type of the scalar value.
     * @return A new instance of {@code ScalarStrategy}.
     */
    @NotNull
    static <E > ScalarStrategy< E > of(Class< E > clazz, @NotNull BiFunction<? super E, ValueIn, E > read) {
        return new ScalarStrategy<>(clazz, read);
    }

    /**
     * Factory method to create a new instance of {@code ScalarStrategy}
     * for text data. This strategy reads text and applies the provided function
     * to convert the text into the desired scalar type.
     *
     * @param clazz The class type of the scalar value.
     * @param func  The function used to convert text into the scalar value.
     * @param <E>   The type of the scalar value.
     * @return A new instance of {@code ScalarStrategy} for text.
     */
    @Nullable
    static <E > ScalarStrategy< E > text(Class< E > clazz, @NotNull Function<String, E > func) {
        return new ScalarStrategy<>(clazz, (Object o, ValueIn in) -> {
            @Nullable String text = in.text();
            return text == null ? null : func.apply(text);
        });
    }

    @NotNull
    @Override
    public BracketType bracketType() {
        // scalar values are typically written without any brackets
        return BracketType.NONE;
    }

    /**
     * Creates a new instance of the scalar type when no existing object is
     * supplied to {@link #readUsing}. Utilises {@link ObjectUtils#newInstance(Class)}.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    @Override
    public <T> T newInstanceOrNull(Class<T> type) {
        return Jvm.uncheckedCast(ObjectUtils.newInstance(this.type));
    }

    @Override
    public Class<E> type() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    /**
     * Reads the scalar value using the configured {@link #read} function.
     * Returns {@code null} if {@link ValueIn#isNull()}.
     */
    public <T> T readUsing(Class<?> clazz, T using, @NotNull ValueIn in, BracketType bracketType) {
        if (in.isNull())
            return null;

        return (T) read.apply((E) using, in);
    }

    @NotNull
    @Override
    /**
     * @return a textual representation including the scalar type name.
     */
    public String toString() {
        return "ScalarStrategy<" + type.getName() + ">";
    }
}
