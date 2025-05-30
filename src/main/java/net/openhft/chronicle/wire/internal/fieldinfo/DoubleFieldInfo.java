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
 * Internal {@link net.openhft.chronicle.wire.FieldInfo} for {@code double}
 * fields using {@link UnsafeMemory} for direct access.
 */
public final class DoubleFieldInfo extends UnsafeFieldInfo {

    /**
     * @param name        field name used in text form
     * @param type        runtime type
     * @param bracketType formatting hint when writing
     * @param reflectField reflection field used for unsafe access
     */
    public DoubleFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field reflectField) {
        super(name, type, bracketType, reflectField);
    }

    @Override
    /**
     * @return the field value or {@code Double.NaN} if the offset is unavailable
     */
    public double getDouble(Object target) {
        try {
            return UnsafeMemory.unsafeGetDouble(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(DoubleFieldInfo.class, e);
            return Double.NaN;
        }
    }

    @Override
    /** Writes the value using {@link UnsafeMemory}. */
    public void set(Object target, double value) throws IllegalArgumentException {
        try {
            UnsafeMemory.unsafePutDouble(target, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    /** Compares the double values in {@code a} and {@code b}. */
    public boolean isEqual(Object a, Object b) {
        return getDouble(a) == getDouble(b);
    }

    @Override
    /** Copies the double value from {@code src} to {@code dst}. */
    public void copy(Object src, Object dst) {
        set(dst, getDouble(src));
    }
}
