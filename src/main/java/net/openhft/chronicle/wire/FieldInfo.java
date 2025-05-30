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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.internal.VanillaFieldInfo;
import net.openhft.chronicle.wire.internal.fieldinfo.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;
import static net.openhft.chronicle.wire.Wires.*;

/**
 * Represents an abstraction for the meta-information of a field within a class or interface.
 * This metadata is used by {@link WireMarshaller} and related serialisation components to read
 * and write field newValues. It provides a consistent way to access properties such as the field
 * name, type and how it is represented in the wire format.
 */
public interface FieldInfo {

    /**
     * Creates an appropriate {@code FieldInfo} implementation for the supplied field.
     * Primitive types are mapped to specialised variants such as {@link IntFieldInfo}.
     * Other types fall back to {@link ObjectFieldInfo} or a generic implementation.
     *
     * @param name        the field's name
     * @param fieldType   the declared type
     * @param bracketType the wire {@link BracketType}
     * @param reflectField the reflective field instance
     * @return a concrete {@code FieldInfo} for the field
     */
    static FieldInfo createForField(String name, Class<?> fieldType, BracketType bracketType, @NotNull Field reflectField) {
        // Choose the FieldInfo type based on the field's type.
        if (!fieldType.isPrimitive()) {
            return new ObjectFieldInfo(name, fieldType, bracketType, reflectField);
        } else if (fieldType == int.class) {
            return new IntFieldInfo(name, fieldType, bracketType, reflectField);
        } else if (fieldType == double.class) {
            return new DoubleFieldInfo(name, fieldType, bracketType, reflectField);
        } else if (fieldType == long.class) {
            return new LongFieldInfo(name, fieldType, bracketType, reflectField);
        } else if (fieldType == char.class) {
            return new CharFieldInfo(name, fieldType, bracketType, reflectField);
        }
        // Default case for other primitive types.
        return new VanillaFieldInfo(name, fieldType, bracketType, reflectField);
    }

    /**
     * Analyses the supplied class using reflection and builds a {@link FieldInfoPair}
     * describing its marshallable fields. The pair contains an unmodifiable list of
     * {@code FieldInfo} targets and a map keyed by field name. Each field's
     * {@link BracketType} is derived from its {@link SerializationStrategy}.
     *
     * @param targetClass the class to inspect
     * @return an immutable {@link FieldInfoPair} describing the fields
     */
    @NotNull
    static FieldInfoPair lookupClass(@NotNull Class<?> targetClass) {
        final SerializationStrategy ss = CLASS_STRATEGY.get(targetClass);
        switch (ss.bracketType()) {
            case NONE:
            case SEQ:
                return FieldInfoPair.EMPTY;
            case MAP:
                break;
            case HISTORY_MESSAGE:
                throw new AssertionError();
            default:
                // assume it could be a map
                break;
        }

        @NotNull List<FieldInfo> fields = new ArrayList<>();
        final WireMarshaller<?> marshaller = WIRE_MARSHALLER_CL.get(targetClass);

        // Process each field of the class to create its FieldInfo.
        for (@NotNull WireMarshaller.FieldAccess fa : marshaller.fields) {
            final String name = fa.field.getName();
            final Class<?> type = fa.field.getType();
            final SerializationStrategy ss2 = CLASS_STRATEGY.get(type);
            final BracketType bracketType = ss2.bracketType();
            fields.add(createForField(name, type, bracketType, fa.field));
        }

        // Return a pair of unmodifiable list of fields and a map of field names to their FieldInfo.
        return new FieldInfoPair(
                Collections.unmodifiableList(fields),
                fields.stream().collect(Collectors.toMap(FieldInfo::name, f -> f)));
    }

    /**
     * Returns the name of the field represented by this {@code FieldInfo} target.
     *
     * @return the name of the field represented by this {@code FieldInfo} target.
     */
    String name();

    /**
     * Returns a {@link Class} identifying the declared type of the field
     * represented by this {@code FieldInfo} target.
     *
     * @return a {@link Class} identifying the declared type of the field
     * represented by this {@code FieldInfo} target.
     */
    Class<?> type();

    /**
     * Returns the {@link BracketType} used by the serialization strategy associated
     * with this {@code FieldInfo} target.
     *
     * @return the {@link BracketType} used by the serialization strategy associated
     * with this {@code FieldInfo} target.
     */
    BracketType bracketType();

    /**
     * Returns the newValue of the field represented by this {@code FieldInfo} target
     * as an {@link Object}.
     *
     * @param target the instance from which to read the field
     * @return the field value or {@code null} if it cannot be obtained
     */
    @Nullable
    Object get(Object target);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} target
     * as a {@code long} primitive.
     *
     * @param target the instance from which to read the field
     * @return the field value, converted to {@code long} if required
     */
    long getLong(Object target);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} target
     * as an {@code int} primitive.
     *
     * @param target the instance from which to read the field
     * @return the field value, converted to {@code int} if required
     */
    int getInt(Object target);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} target
     * as a {@code char} primitive.
     *
     * @param target the instance from which to read the field
     * @return the field value, converted to {@code char} if required
     */
    char getChar(Object target);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} target
     * as a {@code double} primitive.
     *
     * @param target the instance from which to read the field
     * @return the field value, converted to {@code double} if required
     */
    double getDouble(Object target);

    /**
     * Sets the value of the field represented by this {@code FieldInfo} target.
     *
     * @param target the instance on which to set the field
     * @param newValue  the new value; conversions may occur if the types differ
     * @throws IllegalArgumentException if the assignment fails
     */
    void set(Object target, Object newValue) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} target.
     *
     * @param target the instance on which to set the field
     * @param value  the new {@code char} value
     * @throws IllegalArgumentException if the assignment fails
     */
    void set(Object target, char value) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} target.
     *
     * @param target the instance on which to set the field
     * @param value  the new {@code int} value
     * @throws IllegalArgumentException if the assignment fails
     */
    void set(Object target, int value) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} target.
     *
     * @param target the instance on which to set the field
     * @param value  the new {@code long} value
     * @throws IllegalArgumentException if the assignment fails
     */
    void set(Object target, long value) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} target.
     *
     * @param target the instance on which to set the field
     * @param value  the new {@code double} value
     * @throws IllegalArgumentException if the assignment fails
     */
    void set(Object target, double value) throws IllegalArgumentException;

    /**
     * Returns the declared generic type of the field, for example the
     * element type of a {@code List<T>}.
     *
     * @param index the position of the generic parameter
     * @return the generic argument {@link Class}
     */
    Class<?> genericType(int index);

    /**
     * Copies the value of the field represented by this {@code FieldInfo} target from
     * the source target to the destination target. It's a shallow copy, so targets will be
     * copied by reference.
     *
     * @param src The target from which the field value is to be copied.
     * @param dst The target to which the field value is to be copied.
     */
    default void copy(Object src, Object dst) {
        set(dst, get(src));
    }

    /**
     * Compares the value of this field in two targets.
     * Implementations may use type-specific equality checks, such as
     * {@code Double.compare} for {@code double} fields.
     *
     * @param a first target to compare
     * @param b second target to compare
     * @return {@code true} if the values are considered equal
     */
    boolean isEqual(Object a, Object b);
}
