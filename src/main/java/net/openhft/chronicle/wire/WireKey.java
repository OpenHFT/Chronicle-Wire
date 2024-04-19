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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a unique identifier or key for wiring protocols. This interface can
 * be used to encode or decode structured data, and it provides methods for getting
 * the name and the default value of the key, as well as utility methods for
 * working with the key's code.
 */
@FunctionalInterface
public interface WireKey {

    /**
     * Checks if the provided array of WireKey objects have unique codes.
     * Throws an AssertionError if two or more keys have the same code.
     *
     * @param keys An array of WireKey objects to check.
     * @return Returns true if all keys have unique codes.
     * @throws AssertionError if two or more keys have the same code.
     */
    static boolean checkKeys(@NotNull WireKey[] keys) {
        @NotNull Map<Integer, WireKey> codes = new HashMap<>();
        for (@NotNull WireKey key : keys) {
            WireKey pkey = codes.put(key.code(), key);
            if (pkey != null)
                throw new AssertionError(pkey + " and " + key + " have the same code " + key.code());
        }
        return true;
    }

    /**
     * Converts the provided CharSequence into a code. If the CharSequence starts
     * with a digit, it attempts to parse it as an integer. Otherwise, it returns
     * the hash code of the CharSequence.
     *
     * @param cs CharSequence to convert.
     * @return The converted code.
     */
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

    /**
     * Retrieves the name of the WireKey.
     *
     * @return Name of the WireKey.
     */
    @NotNull
    CharSequence name();

    /**
     * Calculates the code of the WireKey based on its name.
     * By default, it uses the {@link #toCode(CharSequence)} method.
     *
     * @return Code of the WireKey.
     */
    default int code() {
        return toCode(name());
    }

    /**
     * Determines the type of the WireKey based on its default value.
     * If the default value is null, it returns Void.class.
     *
     * @return Type of the WireKey.
     */
    default Type type() {
        @Nullable Object o = defaultValue();
        return o == null ? Void.class : o.getClass();
    }

    /**
     * Retrieves the default value associated with this WireKey.
     *
     * @return Default value of the WireKey, or null if not defined.
     */
    @Nullable
    default Object defaultValue() {
        return null;
    }

    /**
     * Checks if the provided CharSequence content matches the string representation
     * of this WireKey.
     *
     * @param c CharSequence to compare with.
     * @return True if content matches, otherwise false.
     */
    default boolean contentEquals(@NotNull CharSequence c) {
        return this.toString().contentEquals(c);
    }
}
