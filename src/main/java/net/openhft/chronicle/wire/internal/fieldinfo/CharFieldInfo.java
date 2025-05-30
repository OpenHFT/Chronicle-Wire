/*
 * Copyright 2016-2025 chronicle.software
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

package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Internal {@link net.openhft.chronicle.wire.FieldInfo} for {@code char} fields
 * using {@link UnsafeMemory} to read and write values via memory offsets.
 */
public final class CharFieldInfo extends UnsafeFieldInfo {

    /**
     * @param name        field name used in text form
     * @param type        runtime type
     * @param bracketType formatting hint when writing
     * @param reflectField reflection field used for unsafe access
     */
    public CharFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field reflectField) {
        super(name, type, bracketType, reflectField);
    }

    @Override
    /**
     * @return the {@code char} value or {@link Character#MAX_VALUE} if the
     * offset could not be determined
     */
    public char getChar(Object target) {
        try {
            return UnsafeMemory.unsafeGetChar(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(CharFieldInfo.class, e);
            return Character.MAX_VALUE;
        }
    }

    @Override
    /** Writes the value using {@link UnsafeMemory}. */
    public void set(Object target, char value) throws IllegalArgumentException {
        try {
            UnsafeMemory.unsafePutChar(target, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    /** Compares the character values in {@code a} and {@code b}. */
    public boolean isEqual(Object a, Object b) {
        return getChar(a) == getChar(b);
    }

    @Override
    /** Copies the character value from {@code src} to {@code dst}. */
    public void copy(Object src, Object dst) {
        set(dst, getChar(src));
    }
}
