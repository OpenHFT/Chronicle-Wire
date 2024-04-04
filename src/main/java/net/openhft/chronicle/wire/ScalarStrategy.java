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

class ScalarStrategy<E> implements SerializationStrategy {
    @NotNull
    final BiFunction<? super E, ValueIn, E> read;
    private final Class<E> type;

    ScalarStrategy(Class<E> type, @NotNull BiFunction<? super E, ValueIn, E> read) {
        this.type = type;
        this.read = read;
    }

    @NotNull
    static <T> ScalarStrategy<T> of(Class<T> clazz, @NotNull BiFunction<? super T, ValueIn, T> read) {
        return new ScalarStrategy<>(clazz, read);
    }

    @Nullable
    static <T> ScalarStrategy<T> text(Class<T> clazz, @NotNull Function<String, T> func) {
        return new ScalarStrategy<>(clazz, (Object o, ValueIn in) -> {
            @Nullable String text = in.text();
            return text == null ? null : func.apply(text);
        });
    }

    @NotNull
    @Override
    public BracketType bracketType() {
        return BracketType.NONE;
    }

    @SuppressWarnings("rawtypes")
    @NotNull
    @Override
    public <T> T newInstanceOrNull(Class<T>type) {
        return Jvm.uncheckedCast(ObjectUtils.newInstance(this.type));
    }

    @Override
    public Class<E> type() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T readUsing(Class<?> clazz, T using, @NotNull ValueIn in, BracketType bracketType) {
        if (in.isNull())
            return null;

        return (T) read.apply((E) using, in);
    }

    @NotNull
    @Override
    public String toString() {
        return "ScalarStrategy<" + type.getName() + ">";
    }
}
