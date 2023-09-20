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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.StringUtils;

/**
 * This class provides a simple hash map implementation optimized for {@link CharSequence} keys and generic values.
 * The main advantage of this implementation is that it's tailored for CharSequence inputs, allowing for a more
 * memory-efficient and performant solution in specific use-cases.
 *
 * @param <T> the type of values to be stored in the map.
 * @since 2023-09-14
 */
public class CharSequenceObjectMap<T> {

    // Constants used in the hash function for improved distribution.
    private static final int K0 = 0x6d0f27bd;
    @SuppressWarnings("unused")
    private static final int M0 = 0x5bc80bad;

    // Arrays to store the keys and corresponding values.
    final String[] keys;
    final T[] values;
    final int mask;  // The mask used for wrapping hash values within the bounds.

    /**
     * Constructs an empty map with the specified initial capacity.
     *
     * @param capacity the initial capacity of the map.
     */
    @SuppressWarnings("unchecked")
    public CharSequenceObjectMap(int capacity) {
        int nextPower2 = Maths.nextPower2(capacity, 16);
        keys = new String[nextPower2];
        values = (T[]) new Object[nextPower2];
        mask = nextPower2 - 1;
    }

    /**
     * Associates the specified value with the specified key (name) in this map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param name key with which the specified value is to be associated.
     * @param t    value to be associated with the specified key.
     * @throws IllegalStateException if the map is full.
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
     * Returns the value to which the specified key (cs) is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param cs the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     * @throws IllegalStateException if the map is full.
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
     * @param name the CharSequence for which the hash code is to be calculated.
     * @return the calculated hash code.
     */
    private int hashFor(CharSequence name) {
        long h = name.length();
        for (int i = 0; i < name.length(); i++) {
            h = h * K0 + name.charAt(i);  // Combine the hash with individual characters.
        }
        return (int) Maths.agitate(h) & mask;  // Agitate and mask the hash value.
    }
}
