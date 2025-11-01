/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.StringUtils;

/**
 * Simple hash map implementation optimised for {@link CharSequence} keys.
 * It uses open addressing with linear probing and stores key strings directly.
 * Not intended as a general-purpose replacement for {@link java.util.HashMap},
 * but can be beneficial in performance-sensitive components where lookups on
 * {@link CharSequence} instances are frequent.  This implementation is not
 * thread-safe.
 *
 * @param <T> type of values stored in the map
 */
public class CharSequenceObjectMap<T> {

    // Constants used in hashFor(CharSequence) for improved distribution
    private static final int K0 = 0x6d0f27bd;
    @SuppressWarnings("unused")
    private static final int M0 = 0x5bc80bad;

    // Array storing the string representations of the keys. null entries indicate empty slots
    final String[] keys;

    // Array storing the values associated with the keys at the same index.
    final T[] values;

    // Mask used for mapping hash codes to array indices. Equal to the array length minus one
    final int mask;

    /**
     * Creates a map with an initial table size that is the next power of two
     * greater than or equal to {@code capacity} with a minimum of sixteen.
     *
     * @param capacity desired initial capacity before rounding
     */
    @SuppressWarnings("unchecked")
    public CharSequenceObjectMap(int capacity) {
        int nextPower2 = Maths.nextPower2(capacity, 16);
        keys = new String[nextPower2];
        values = (T[]) new Object[nextPower2];
        mask = nextPower2 - 1;
    }

    /**
     * Store a value against a key. The key's {@code toString()} value is stored
     * if it is not already present, replacing any existing mapping.
     *
     * @param name the key
     * @param t    value to store
     * @throws IllegalStateException if no empty slot can be found after probing
     */
    public void put(CharSequence name, T t) {
        int h = hashFor(name);
        for (int i = 0; i < mask; i++) {
            if (keys[i] == null || StringUtils.isEqual(keys[i], name)) {
                keys[i] = name.toString();
                values[i] = t;
                return;
            }
            h = (h + 1) & mask;  // Increment the hash and wrap it.
        }
        throw new IllegalStateException("Map is full");
    }

    /**
     * Retrieve the value associated with a key.
     *
     * @param cs the key to search for
     * @return the mapped value, or {@code null} if the key is absent or the map is
     *         full and the key cannot be located
     * @throws IllegalStateException if probing finds no empty slot
     */
    public T get(CharSequence cs) {
        int h = hashFor(cs);
        for (int i = 0; i < mask; i++) {
            if (keys[i] == null)
                return null;
            if (StringUtils.isEqual(keys[i], cs))
                return values[i];
            h = (h + 1) & mask;  // Increment the hash and wrap it.
        }
        throw new IllegalStateException("Map is full");
    }

    /**
     * Generates a hash code for the given CharSequence using a custom hash function.
     * This custom function is designed to produce well-distributed hash codes for CharSequence inputs.
     *
     * @param name the {@link CharSequence} to hash
     * @return an int masked to the current table size
     */
    private int hashFor(CharSequence name) {
        long h = name.length();
        for (int i = 0; i < name.length(); i++) {
            h = h * K0 + name.charAt(i);  // Combine the hash with individual characters.
        }
        return (int) Maths.agitate(h) & mask;  // Agitate and mask the hash value.
    }
}
