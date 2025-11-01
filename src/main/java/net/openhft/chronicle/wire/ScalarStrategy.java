/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implements the {@link SerializationStrategy} for scalar data types.
 * This strategy is designed for simple types and provides mechanisms for reading
 * them using provided functions.
 *
 * @param <E> The type of the scalar value that this strategy handles.
 */
class ScalarStrategy<E> implements SerializationStrategy {
    /**
     * The {@link BiFunction} used to deserialize the scalar value. It takes an
     * optional existing object (or {@code null}) and the {@link ValueIn} source,
     * and returns the deserialized value.
     */
    final BiFunction<? super E, ValueIn, E> read;
    // The class type of the scalar value
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

    /**
     * Returns {@link BracketType#NONE}, as scalar values are typically not enclosed
     * in structural brackets.
     */
    @NotNull
    @Override
    public BracketType bracketType() {
        return BracketType.NONE;
    }

    /**
     * Creates a new instance of the scalar type using {@link ObjectUtils#newInstance(Class)}.
     * This is used when no existing object is provided for deserialization.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    @Override
    public <T> T newInstanceOrNull(Class<T>type) {
        return Jvm.uncheckedCast(ObjectUtils.newInstance(this.type));
    }

    /**
     * Returns the {@link Class} of the scalar type this strategy handles.
     */
    @Override
    public Class<E> type() {
        return type;
    }

    /**
     * Reads the scalar value using the configured {@link #read} function. Returns
     * {@code null} if the input {@link ValueIn#isNull()}.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T readUsing(Class<?> clazz, T using, @NotNull ValueIn in, BracketType bracketType) {
        if (in.isNull())
            return null;

        return (T) read.apply((E) using, in);
    }

    /**
     * @return A string representation of this strategy, including the scalar type name.
     */
    @NotNull
    @Override
    public String toString() {
        return "ScalarStrategy<" + type.getName() + ">";
    }
}
