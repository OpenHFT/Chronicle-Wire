/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a unique identifier or key for wiring protocols.
 * This is a functional interface where {@link #name()} is the primary method.
 * Implementations are typically enums or small classes.
 * A {@code WireKey} associates a textual name with a numeric {@link #code()} for use in binary wire formats.
 */
@FunctionalInterface
public interface WireKey {

    /**
     * Checks that each entry uses a distinct {@link #code()} value.
     *
     * @param keys An array of {@code WireKey} objects to be checked for unique {@link #code()} values.
     * @return {@code true} if all codes are unique.
     * @throws AssertionError if two or more keys share the same code.
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
     * Converts a textual name to a numeric code.
     * If the text begins with a digit it is parsed as an integer, otherwise its hash code is used.
     *
     * @param cs The {@link CharSequence} (typically a field or event name) to convert into a numeric code.
     * @return the derived code.
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
     * Returns the textual name of this key.
     * This is often used in text-based wire formats or for debugging.
     *
     * @return the name of this key.
     */
    @NotNull
    CharSequence name();

    /**
     * Returns a numeric code for this key, typically derived from its {@link #name()}.
     * By default this delegates to {@link #toCode(CharSequence)}.
     *
     * @return the code for this key.
     */
    default int code() {
        return toCode(name());
    }

    /**
     * Returns the {@link java.lang.reflect.Type} associated with the value this key typically represents,
     * inferred from {@link #defaultValue()}. Returns {@code Void.class} if no default value is defined.
     *
     * @return the associated type.
     */
    default Type type() {
        @Nullable Object o = defaultValue();
        return o == null ? Void.class : o.getClass();
    }

    /**
     * Returns the default value for the field or event represented by this key.
     * Deserialisers may use this if the key is absent. {@code null} indicates no default.
     *
     * @return the default value or {@code null}.
     */
    @Nullable
    default Object defaultValue() {
        return null;
    }

    /**
     * Compares the string representation of this {@code WireKey} (obtained via {@code this.toString()})
     * with the provided {@link CharSequence} {@code c} for content equality.
     *
     * @param c The {@link CharSequence} to compare against this key's string form.
     * @return {@code true} if the contents are equal.
     */
    default boolean contentEquals(@NotNull CharSequence c) {
        return this.toString().contentEquals(c);
    }
}
