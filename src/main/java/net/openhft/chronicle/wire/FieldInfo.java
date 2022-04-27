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

import net.openhft.chronicle.wire.internal.fieldinfo.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;

/**
 * Provides information about a single field of a class or an interface.
 */
public interface FieldInfo {

    @SuppressWarnings("deprecation" /* VanillaFieldInfo will become internal in the future, remove this suppression in x.25 */)
    static FieldInfo createForField(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        if (!type.isPrimitive()) {
            return new ObjectFieldInfo(name, type, bracketType, field);
        } else if (type == int.class) {
            return new IntFieldInfo(name, type, bracketType, field);
        } else if (type == double.class) {
            return new DoubleFieldInfo(name, type, bracketType, field);
        } else if (type == long.class) {
            return new LongFieldInfo(name, type, bracketType, field);
        } else if (type == char.class) {
            return new CharFieldInfo(name, type, bracketType, field);
        }
        return new VanillaFieldInfo(name, type, bracketType, field);
    }

    @NotNull
    static Wires.FieldInfoPair lookupClass(@NotNull Class<?> aClass) {
        final SerializationStrategy<?> ss = Wires.CLASS_STRATEGY.get(aClass);
        switch (ss.bracketType()) {
            case NONE:
            case SEQ:
                return Wires.FieldInfoPair.EMPTY;
            case MAP:
                break;
            default:
                // assume it could be a map
                break;
        }

        @NotNull List<FieldInfo> fields = new ArrayList<>();
        final WireMarshaller<?> marshaller = WIRE_MARSHALLER_CL.get(aClass);
        for (@NotNull WireMarshaller.FieldAccess fa : marshaller.fields) {
            final String name = fa.field.getName();
            final Class<?> type = fa.field.getType();
            final SerializationStrategy<?> ss2 = Wires.CLASS_STRATEGY.get(type);
            final BracketType bracketType = ss2.bracketType();
            fields.add(createForField(name, type, bracketType, fa.field));
        }
        return new Wires.FieldInfoPair(
                Collections.unmodifiableList(fields),
                fields.stream().collect(Collectors.toMap(FieldInfo::name, f -> f)));
    }

    /**
     * Returns the name of the field represented by this {@code FieldInfo} object.
     *
     * @return the name of the field represented by this {@code FieldInfo} object.
     */
    String name();

    /**
     * Returns a {@link Class} identifying the declared type of the field
     * represented by this {@code FieldInfo} object.
     *
     * @return a {@link Class} identifying the declared type of the field
     * represented by this {@code FieldInfo} object.
     */
    @SuppressWarnings("rawtypes")
    Class<?> type();

    /**
     * Returns the {@link BracketType} used by the serialization strategy associated
     * with this {@code FieldInfo} object.
     *
     * @return the {@link BracketType} used by the serialization strategy associated
     * with this {@code FieldInfo} object.
     */
    BracketType bracketType();

    /**
     * Returns the value of the field represented by this {@code FieldInfo} object
     * as an {@link Object}. The provided {@code object} is used as a target to
     * extract the field from.
     *
     * @return the value of the field represented by this {@code FieldInfo} object
     * as an {@link Object}.
     */
    @Nullable
    Object get(Object object);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} object
     * as a {@code long} primitive. The provided {@code object} is used as a target
     * to extract the field from.
     *
     * @return the value of the field represented by this {@code FieldInfo} object
     * as a {@code long} primitive.
     */
    long getLong(Object object);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} object
     * as an {@code int} primitive. The provided {@code object} is used as a target
     * to extract the field from.
     *
     * @return the value of the field represented by this {@code FieldInfo} object
     * as an {@code int} primitive.
     */
    int getInt(Object object);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} object
     * as a {@code char} primitive. The provided {@code object} is used as a target
     * to extract the field from.
     *
     * @return the value of the field represented by this {@code FieldInfo} object
     * as a {@code char} primitive.
     */
    char getChar(Object object);

    /**
     * Returns the value of the field represented by this {@code FieldInfo} object
     * as a {@code double} primitive. The provided {@code object} is used as a target
     * to extract the field from.
     *
     * @return the value of the field represented by this {@code FieldInfo} object
     * as a {@code double} primitive.
     */
    double getDouble(Object object);

    /**
     * Sets the value of the field represented by this {@code FieldInfo} object
     * to the provided {@code value}. The provided {@code object} is used as a
     * target to the extract eh field from.
     */
    void set(Object object, Object value) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} object
     * to the provided {@code value}. The provided {@code object} is used as a
     * target to the extract eh field from.
     */
    void set(Object object, char value) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} object
     * to the provided {@code value}. The provided {@code object} is used as a
     * target to the extract eh field from.
     */
    void set(Object object, int value) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} object
     * to the provided {@code value}. The provided {@code object} is used as a
     * target to the extract eh field from.
     */
    void set(Object object, long value) throws IllegalArgumentException;

    /**
     * Sets the value of the field represented by this {@code FieldInfo} object
     * to the provided {@code value}. The provided {@code object} is used as a
     * target to the extract eh field from.
     */
    void set(Object object, double value) throws IllegalArgumentException;

    /**
     * Returns a {@link Class} identifying the declared generic type of the
     * field represented by this {@code FieldInfo} object.
     *
     * @return a {@link Class} identifying the declared generic type of the
     * field represented by this {@code FieldInfo} object.
     */
    Class<?> genericType(int index);

    /**
     * Copy the field from the source object to the destination
     * <p>
     * Note: this is not a deep copy, objects will be copied by reference
     *
     * @param source      The object to copy the field value from
     * @param destination The object to copy the field value to
     */
    default void copy(Object source, Object destination) {
        set(destination, get(source));
    }

    boolean isEqual(Object a, Object b);
}