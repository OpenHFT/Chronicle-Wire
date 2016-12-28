/*
 * Copyright 2016 higherfrequencytrading.com
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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter.lawrey on 1/10/15.
 */
@FunctionalInterface
public interface WireKey {

    static boolean checkKeys(@NotNull WireKey[] keys) {
        @NotNull Map<Integer, WireKey> codes = new HashMap<>();
        for (@NotNull WireKey key : keys) {
            WireKey pkey = codes.put(key.code(), key);
            if (pkey != null)
                throw new AssertionError(pkey + " and " + key + " have the same code " + key.code());
        }
        return true;
    }

    static int toCode(@NotNull CharSequence cs) {
        @NotNull String s = cs.toString();
        if (s.length() > 0 && Character.isDigit(s.charAt(0)))
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException faillback) {
                // ignored
            }
        return s.hashCode();
    }

    @NotNull
    CharSequence name();

    default int code() {
        return toCode(name());
    }

    default Type type() {
        @Nullable Object o = defaultValue();
        return o == null ? Void.class : o.getClass();
    }

    @Nullable
    default Object defaultValue() {
        return null;
    }

    default boolean contentEquals(@NotNull CharSequence c) {
        return this.toString().contentEquals(c);
    }
}
