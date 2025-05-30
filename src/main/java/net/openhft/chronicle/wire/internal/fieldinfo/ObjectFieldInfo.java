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
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Internal {@link net.openhft.chronicle.wire.FieldInfo} for reference fields
 * using {@link UnsafeMemory} for direct access.
 */
public final class ObjectFieldInfo extends UnsafeFieldInfo {

    /**
     * @param name        field name used in text form
     * @param type        runtime type
     * @param bracketType formatting hint when writing
     * @param reflectField reflection field used for unsafe access
     */
    public ObjectFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field reflectField) {
        super(name, type, bracketType, reflectField);
    }

    @Override
    /**
     * @return the field value or {@code null} if the offset could not be determined
     */
    public @Nullable Object get(Object target) {
        try {
            return UnsafeMemory.unsafeGetObject(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(ObjectFieldInfo.class, e);
            return null;
        }
    }

    @Override
    /** Writes the value using {@link UnsafeMemory}, converting as required. */
    public void set(Object target, Object newValue) throws IllegalArgumentException {
        Object value2 = ObjectUtils.convertTo(type, newValue);
        try {
            UnsafeMemory.unsafePutObject(target, getOffset(), value2);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    /** Compares the object values in {@code a} and {@code b}. */
    public boolean isEqual(Object a, Object b) {
        return Objects.deepEquals(get(a), get(b));
    }

    @Override
    /** Copies the object value from {@code src} to {@code dst}. */
    public void copy(Object src, Object dst) {
        set(dst, get(src));
    }
}
