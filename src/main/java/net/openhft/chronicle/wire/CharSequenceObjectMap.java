/*
 * Copyright 2016-2020 Chronicle Software
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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.StringUtils;

public class CharSequenceObjectMap<T> {
    private static final int K0 = 0x6d0f27bd;
    @SuppressWarnings("unused")
    private static final int M0 = 0x5bc80bad;

    final String[] keys;
    final T[] values;
    final int mask;

    @SuppressWarnings("unchecked")
    public CharSequenceObjectMap(int capacity) {
        int nextPower2 = Maths.nextPower2(capacity, 16);
        keys = new String[nextPower2];
        values = (T[]) new Object[nextPower2];
        mask = nextPower2 - 1;
    }

    public void put(CharSequence name, T t) {
        int h = hashFor(name);
        for (int i = 0; i < mask; i++) {
            if (keys[i] == null || StringUtils.isEqual(keys[i], name)) {
                keys[i] = name.toString();
                values[i] = t;
                return;
            }
            h = (h + 1) & mask;
        }
        throw new IllegalStateException("Map is full");
    }

    public T get(CharSequence cs) {
        int h = hashFor(cs);
        for (int i = 0; i < mask; i++) {
            if (keys[i] == null)
                return null;
            if (StringUtils.isEqual(keys[i], cs))
                return values[i];
            h = (h + 1) & mask;
        }
        throw new IllegalStateException("Map is full");
    }

    private int hashFor(CharSequence name) {
        long h = name.length();
        for (int i = 0; i < name.length(); i++) {
            h = h * K0 + name.charAt(i);
        }
        return (int) Maths.agitate(h) & mask;
    }
}
