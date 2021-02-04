/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.Maths.hash32;

/**
 * This cache only gaurentees it will provide a String which matches the decoded bytes.
 * <p>
 * It doesn't guantee it will always return the same object,
 * nor that different threads will return the same object,
 * though the contents should always be the same.
 * <p>
 * While not technically thread safe, it should still behave correctly.
 *
 * @author peter.lawrey
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class FromStringInterner<T> {
    protected final InternerEntry<T>[] entries;
    protected final int mask, shift;
    protected boolean toggle = false;

    public FromStringInterner(int capacity) throws IllegalArgumentException {
        int n = Maths.nextPower2(capacity, 128);
        shift = Maths.intLog2(n);
        entries = new InternerEntry[n];
        mask = n - 1;
    }

    public T intern(@NotNull String s)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException {
        int hash = hash32(s);
        int h = hash & mask;
        InternerEntry<T> ie = entries[h];
        int length = s.length();
        if (ie != null && ie.key.length() == length && ie.key.equals(s))
            return ie.t;
        int h2 = (hash >> shift) & mask;
        InternerEntry<T> s2 = entries[h2];
        if (s2 != null && s2.key.length() == length && s2.key.equals(s))
            return s2.t;
        @NotNull T t = getValue(s);
        entries[ie == null || (s2 != null && toggle()) ? h : h2] = new InternerEntry<>(s, t);
        return t;
    }

    @NotNull
    protected abstract T getValue(String s) throws IORuntimeException;

    protected boolean toggle() {
        return toggle = !toggle;
    }

    static class InternerEntry<T> {
        final String key;
        final T t;

        InternerEntry(String key, T t) {
            this.key = key;
            this.t = t;
        }
    }
}
