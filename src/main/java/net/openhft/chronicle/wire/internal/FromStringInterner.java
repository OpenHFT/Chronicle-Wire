/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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

package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

/**
 * A cache designed to provide a unique representation for equivalent string values,
 * primarily to reduce the memory footprint when dealing with multiple identical strings.
 * <p>
 * This cache guarantees that it will provide a value matching the decoded bytes of the input string,
 * but doesn't ensure the same object is returned on subsequent calls or across threads.
 * </p>
 * <p>
 * Note: While it's not strictly thread-safe, it's expected to still produce correct results.
 * </p>
 * @author peter.lawrey
 */
@SuppressWarnings("unchecked")
public abstract class FromStringInterner<T> {

    // Array of interned entries
    protected final InternerEntry<T>[] entries;

    // Mask and shift values for hash calculations
    protected final int mask;
    protected final int shift;

    // Toggle for choosing between hash locations in case of a collision
    protected boolean toggle = false;

    /**
     * Constructor initializes the entries array and calculates mask and shift values.
     *
     * @param capacity The desired capacity of the interner.
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    protected FromStringInterner(int capacity) throws IllegalArgumentException {
        int n = Maths.nextPower2(capacity, 128);
        shift = Maths.intLog2(n);
        entries = Jvm.uncheckedCast(new InternerEntry[n]);
        mask = n - 1;
    }

    /**
     * Returns the interned value for the provided string.
     *
     * @param s The string to be interned.
     * @return The interned representation.
     * @throws IllegalArgumentException, IORuntimeException, BufferUnderflowException
     */
    public T intern(@NotNull String s)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException {

        // Calculate the hash of the string
        long h1 = Maths.hash64(s);
        h1 ^= h1 >> 32;
        int h = (int) h1 & mask;

        // Try to fetch the entry from the first hash location
        InternerEntry<T> ie = entries[h];
        int length = s.length();
        if (ie != null && ie.key.length() == length && ie.key.equals(s))
            return ie.t;

        // Try the second hash location in case of a collision
        int h2 = (int) (h1 >> shift) & mask;
        InternerEntry<T> s2 = entries[h2];
        if (s2 != null && s2.key.length() == length && s2.key.equals(s))
            return s2.t;

        // If the string wasn't found, create a new value and store it
        @NotNull T t = getValue(s);
        entries[ie == null || (s2 != null && toggle()) ? h : h2] = new InternerEntry<>(s, t);
        return t;
    }

    /**
     * Abstract method to retrieve a value for a given string.
     * Implementations should provide a mechanism to get the desired value type T for a given string.
     *
     * @param s The input string.
     * @return The value representation.
     * @throws IORuntimeException
     */
    @NotNull
    protected abstract T getValue(String s) throws IORuntimeException;

    /**
     * Toggles between true and false. Used for choosing hash locations.
     *
     * @return The toggled value.
     */
    protected boolean toggle() {
        return toggle = !toggle;
    }

    /**
     * Represents an entry in the interner with a key (string) and a value of type T.
     */
    static class InternerEntry<T> {
        final String key; // Original string
        final T t;        // Interned representation

        InternerEntry(String key, T t) {
            this.key = key;
            this.t = t;
        }
    }
}
