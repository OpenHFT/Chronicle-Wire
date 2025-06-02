/*
 * Copyright 2016-2022 chronicle.software
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
 * Represents field information for object fields, extending the generic field information capabilities
 * provided by {@link UnsafeFieldInfo}. This class offers direct memory access functionality to get and set
 * object values in objects, leveraging unsafe operations for performance enhancement.
 */
public final class ObjectFieldInfo extends UnsafeFieldInfo {

    /**
     * Constructs an instance of ObjectFieldInfo with the provided details about an object field.
     *
     * @param name        The name of the field.
     * @param type        The type of the field.
     * @param bracketType The bracket type associated with the field.
     * @param field       The actual field representation.
     */
    public ObjectFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    @Override
    public @Nullable Object get(Object object) {
        try {
            return UnsafeMemory.unsafeGetObject(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(ObjectFieldInfo.class, e);
            return null;
        }
    }

    @Override
    public void set(Object object, Object value) throws IllegalArgumentException {
        Object value2 = ObjectUtils.convertTo(type, value);
        try {
            UnsafeMemory.unsafePutObject(object, getOffset(), value2);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        return Objects.deepEquals(get(a), get(b));
    }

    @Override
    public void copy(Object source, Object destination) {
        set(destination, get(source));
    }
}
