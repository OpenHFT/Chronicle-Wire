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
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.AbstractFieldInfo;
import net.openhft.chronicle.wire.BracketType;
import net.openhft.chronicle.wire.FieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

@SuppressWarnings("rawtypes")
public class VanillaFieldInfo extends AbstractFieldInfo implements FieldInfo {

    private final Class<?> parent;
    private transient Field field;

    public VanillaFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(type, bracketType, name);
        parent = field.getDeclaringClass();
        this.field = field;
    }

    @Nullable
    @Override
    public Object get(Object object) {
        try {
            return getField().get(object);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return null;
        }
    }

    @Override
    public long getLong(Object object) {
        try {
            return getField().getLong(object);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Long.MIN_VALUE;
        }
    }

    @Override
    public int getInt(Object instance) {
        try {
            return getField().getInt(instance);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public char getChar(Object instance) {
        try {
            return getField().getChar(instance);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Character.MAX_VALUE;
        }
    }

    @Override
    public double getDouble(Object object) {
        try {
            return getField().getDouble(object);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Double.NaN;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void set(Object instance, Object value) throws IllegalArgumentException {
        Object value2 = ObjectUtils.convertTo(type, value);
        try {
            getField().set(instance, value2);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object instance, int value) throws IllegalArgumentException {
        try {
            getField().setInt(instance, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object instance, char value) throws IllegalArgumentException {
        try {
            getField().setChar(instance, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object instance, long value) throws IllegalArgumentException {
        try {
            getField().setLong(instance, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object instance, double value) throws IllegalArgumentException {
        try {
            getField().setDouble(instance, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Field getField() throws NoSuchFieldException {
        if (field == null) {
            field = parent.getDeclaredField(name);
            Jvm.setAccessible(field);
        }
        return field;
    }

    @Override
    public Class<?> genericType(int parameterIndex) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type type = genericType.getActualTypeArguments()[parameterIndex];
        return (Class) type;
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        if (type.isPrimitive()) {
            if (type == int.class)
                return getInt(a) == getInt(b);
            if (type == long.class)
                return getLong(a) == getLong(b);
            if (type == double.class)
                return getDouble(a) == getDouble(b);
            if (type == char.class)
                return getChar(a) == getChar(b);
        }
        return Objects.deepEquals(get(a), get(b));
    }
}
