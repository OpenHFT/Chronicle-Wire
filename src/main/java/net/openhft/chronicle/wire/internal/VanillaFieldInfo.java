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

/**
 * Standard reflection-backed implementation of {@link AbstractFieldInfo}.
 * The associated {@link Field} is recreated on demand so that instances
 * remain functional after deserialisation. Intended for internal marshalling
 * tasks where unsafe access is not required.
 */
@SuppressWarnings("rawtypes")
public class VanillaFieldInfo extends AbstractFieldInfo implements FieldInfo {

    /**
     * Declaring class of the target field. Used to re-acquire the
     * {@link #field} instance if needed.
     */
    private final Class<?> parent;

    /**
     * Reflection handle to the actual field. Marked transient and looked up
     * lazily via {@link #getField()}.
     */
    private transient Field field;

    /**
     * Creates an instance for a specific field.
     *
     * @param name         textual name of the field
     * @param type         runtime type of the field
     * @param bracketType  bracket style used when writing
     * @param field        reflection field for direct access
     */
    public VanillaFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(type, bracketType, name);
        parent = field.getDeclaringClass();
        this.field = field;
    }

    /**
     * Retrieves the value of this field from {@code object}. The method logs
     * and returns {@code null} if reflection fails.
     */
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

    /**
     * Reads the primitive {@code long} value.
     *
     * @return the field value or {@code Long.MIN_VALUE} on error
     */
    @Override
    public long getLong(Object object) {
        try {
            return getField().getLong(object);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Long.MIN_VALUE;
        }
    }

    /**
     * Reads the primitive {@code int} value.
     *
     * @return the field value or {@code Integer.MIN_VALUE} on error
     */
    @Override
    public int getInt(Object object) {
        try {
            return getField().getInt(object);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Reads the primitive {@code char} value.
     *
     * @return the field value or {@code Character.MAX_VALUE} on error
     */
    @Override
    public char getChar(Object object) {
        try {
            return getField().getChar(object);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            Jvm.debug().on(VanillaFieldInfo.class, e);
            return Character.MAX_VALUE;
        }
    }

    /**
     * Reads the primitive {@code double} value.
     *
     * @return the field value or {@code Double.NaN} on error
     */
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
    /**
     * Converts {@code value} to the field type and writes it using reflection.
     *
     * @throws IllegalArgumentException if the field cannot be accessed
     */
    @Override
    public void set(Object object, Object value) throws IllegalArgumentException {
        Object value2 = ObjectUtils.convertTo(type, value);
        try {
            getField().set(object, value2);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Writes an {@code int} value to the field.
     *
     * @throws IllegalArgumentException if reflection fails
     */
    @Override
    public void set(Object object, int value) throws IllegalArgumentException {
        try {
            getField().setInt(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Writes a {@code char} value to the field.
     *
     * @throws IllegalArgumentException if reflection fails
     */
    @Override
    public void set(Object object, char value) throws IllegalArgumentException {
        try {
            getField().setChar(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Writes a {@code long} value to the field.
     *
     * @throws IllegalArgumentException if reflection fails
     */
    @Override
    public void set(Object object, long value) throws IllegalArgumentException {
        try {
            getField().setLong(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Writes a {@code double} value to the field.
     *
     * @throws IllegalArgumentException if reflection fails
     */
    @Override
    public void set(Object object, double value) throws IllegalArgumentException {
        try {
            getField().setDouble(object, value);
        } catch (@NotNull NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Lazily retrieves the underlying {@link Field}, re-acquiring it from
     * {@link #parent} when required.
     *
     * @throws NoSuchFieldException if the field does not exist on the parent
     */
    public Field getField() throws NoSuchFieldException {
        if (field == null) {
            field = parent.getDeclaredField(name);
            Jvm.setAccessible(field);
        }
        return field;
    }

    /**
     * Returns the {@link Class} of the generic argument at the given index if
     * this field is parameterised.
     */
    @Override
    public Class<?> genericType(int index) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type type = genericType.getActualTypeArguments()[index];
        return (Class) type;
    }

    /**
     * Compares the value of this field in two objects. Primitive types are
     * compared directly, otherwise {@link Objects#deepEquals(Object, Object)}
     * is used.
     */
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
