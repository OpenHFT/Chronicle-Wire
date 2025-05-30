/*
 * Copyright 2016-2020 chronicle.software
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

/**
 * Represents an abstraction for the meta-information of a field within a class
 * or interface.  Implementations provide details about the type, nature and
 * characteristics of the field.  This metadata is used by components such as
 * {@link WireMarshaller} to dynamically read and write values during
 * serialisation.
 */
public interface FieldInfo {

    /**
     * Create the appropriate {@code FieldInfo} implementation for the supplied
     * field.
     *
     * @param name        the field name
     * @param type        the field type
     * @param bracketType how the value is represented on the wire
     * @param field       the reflective {@link Field}
     * @return a specialised {@code FieldInfo}
     */
    static FieldInfo createForField(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        // Choose the FieldInfo type based on the field's type.
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
        // Default case for other primitive types.
        return new VanillaFieldInfo(name, type, bracketType, field);
    }

    /**
     * Analyse the supplied class using reflection and produce a
     * {@link Wires.FieldInfoPair} describing its marshallable fields.  The
     * returned pair contains a list preserving the declared field order and a
     * map for name based lookups.
     *
     * @param aClass the class for which field info should be created
     * @return metadata describing the marshallable fields of {@code aClass}
     */
    @NotNull
    static Wires.FieldInfoPair lookupClass(@NotNull Class<?> aClass) {
        final SerializationStrategy ss = Wires.CLASS_STRATEGY.get(aClass);
        switch (ss.bracketType()) {
            case NONE:
            case SEQ:
                return Wires.FieldInfoPair.EMPTY;
            case MAP:
                break;
            case HISTORY_MESSAGE:
                throw new AssertionError();
            default:
                // assume it could be a map
                break;
        }

        @NotNull List<FieldInfo> fields = new ArrayList<>();
        final WireMarshaller<?> marshaller = WIRE_MARSHALLER_CL.get(aClass);

        // Process each field of the class to create its FieldInfo.
        for (@NotNull WireMarshaller.FieldAccess fa : marshaller.fields) {
            final String name = fa.field.getName();
            final Class<?> type = fa.field.getType();
            final SerializationStrategy ss2 = Wires.CLASS_STRATEGY.get(type);
            final BracketType bracketType = ss2.bracketType();
            fields.add(createForField(name, type, bracketType, fa.field));
        }

        // Return a pair of unmodifiable list of fields and a map of field names to their FieldInfo.
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
     * Retrieve the field's value from the supplied instance.
     *
     * @param object the instance containing the field
     * @return the value of the field, or {@code null} if it cannot be read
     */
    @Nullable
    Object get(Object object);

    /**
     * Retrieve the field's {@code long} value from the given instance.
     *
     * @param object the instance containing the field
     * @return the {@code long} value read, or a type specific default if not accessible
     */
    long getLong(Object object);

    /**
     * Retrieve the field's {@code int} value from the given instance.
     *
     * @param object the instance containing the field
     * @return the {@code int} value read, or a type specific default if not accessible
     */
    int getInt(Object object);

    /**
     * Retrieve the field's {@code char} value from the given instance.
     *
     * @param object the instance containing the field
     * @return the {@code char} value read, or a type specific default if not accessible
     */
    char getChar(Object object);

    /**
     * Retrieve the field's {@code double} value from the given instance.
     *
     * @param object the instance containing the field
     * @return the {@code double} value read, or a type specific default if not accessible
     */
    double getDouble(Object object);

    /**
     * Set the value of the field on the specified instance.
     *
     * @param object the instance to modify
     * @param value  the new value for the field
     * @throws IllegalArgumentException if the field cannot be written
     */
    void set(Object object, Object value) throws IllegalArgumentException;

    /**
     * Set the {@code char} value of the field on the specified instance.
     *
     * @param object the instance to modify
     * @param value  the new value for the field
     * @throws IllegalArgumentException if the field cannot be written
     */
    void set(Object object, char value) throws IllegalArgumentException;

    /**
     * Set the {@code int} value of the field on the specified instance.
     *
     * @param object the instance to modify
     * @param value  the new value for the field
     * @throws IllegalArgumentException if the field cannot be written
     */
    void set(Object object, int value) throws IllegalArgumentException;

    /**
     * Set the {@code long} value of the field on the specified instance.
     *
     * @param object the instance to modify
     * @param value  the new value for the field
     * @throws IllegalArgumentException if the field cannot be written
     */
    void set(Object object, long value) throws IllegalArgumentException;

    /**
     * Set the {@code double} value of the field on the specified instance.
     *
     * @param object the instance to modify
     * @param value  the new value for the field
     * @throws IllegalArgumentException if the field cannot be written
     */
    void set(Object object, double value) throws IllegalArgumentException;

    /**
     * Return the {@link Class} for the generic type parameter at the given
     * {@code index}.  This is useful for fields declared with generics such as
     * {@code List<String>} or {@code Map<K,V>}.
     *
     * @param index zero based index of the generic argument
     * @return the {@link Class} of the generic type argument
     */
    Class<?> genericType(int index);

    /**
     * Copy the value of this field from {@code source} to {@code destination}.
     * The copy is shallow so object references are transferred rather than
     * cloned.
     *
     * @param source      object to read from
     * @param destination object to write to
     */
    default void copy(Object source, Object destination) {
        set(destination, get(source));
    }

    /**
     * Compare the value of this field in two objects for equality.  Primitive
     * types are compared with their natural operators while objects are compared
     * using {@link java.util.Objects#deepEquals(Object, Object)}.
     *
     * @param a first instance
     * @param b second instance
     * @return {@code true} if the field values are considered equal
     */
    boolean isEqual(Object a, Object b);
}
