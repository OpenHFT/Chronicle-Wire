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

import org.jetbrains.annotations.Nullable;

/**
 * Provides information about a single field of a class or an interface.
 */
public interface FieldInfo {

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
}