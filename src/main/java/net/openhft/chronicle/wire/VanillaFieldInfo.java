/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @deprecated This class will become internal in the future, use {@link FieldInfo#createForField(String, Class, BracketType, Field)} instead.
 */
@Deprecated
@SuppressWarnings("rawtypes")
public class VanillaFieldInfo extends AbstractFieldInfo implements FieldInfo {

    private final Class parent;
    private transient Field field;

    public VanillaFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
        super(type, bracketType, name);
        parent = field.getDeclaringClass();
        this.field = field;
    }

    /**
     * @deprecated Use {@link FieldInfo#lookupClass(Class)} instead (to be removed in x.25)
     */
    @NotNull
    @Deprecated
    public static Wires.FieldInfoPair lookupClass(@NotNull Class aClass) {
        return FieldInfo.lookupClass(aClass);
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
    public int getInt(Object object) {
        try {
            return getField().getInt(object);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public char getChar(Object object) {
        try {
            return getField().getChar(object);
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
    public void set(Object object, Object value) throws IllegalArgumentException {
        Object value2 = ObjectUtils.convertTo(type, value);
        try {
            getField().set(object, value2);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, int value) throws IllegalArgumentException {
        try {
            getField().setInt(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, char value) throws IllegalArgumentException {
        try {
            getField().setChar(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, long value) throws IllegalArgumentException {
        try {
            getField().setLong(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void set(Object object, double value) throws IllegalArgumentException {
        try {
            getField().setDouble(object, value);
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
    public Class<?> genericType(int index) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type type = genericType.getActualTypeArguments()[index];
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
