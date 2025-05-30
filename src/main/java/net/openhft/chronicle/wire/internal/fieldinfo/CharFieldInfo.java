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
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * FieldInfo implementation for {@code char} fields using {@link UnsafeMemory}
 * for high performance direct access.
 */
public final class CharFieldInfo extends UnsafeFieldInfo {

    /**
     * Constructs an instance of CharFieldInfo with the provided details about a character field.
     *
     * @param name        The name of the field.
     * @param type        The type of the field.
     * @param bracketType The bracket type associated with the field.
     * @param field       The field object representation.
     */
    public CharFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    /**
     * Read the {@code char} value directly using an unsafe memory read.
     */
    @Override
    public char getChar(Object object) {
        try {
            return UnsafeMemory.unsafeGetChar(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(CharFieldInfo.class, e);
            return Character.MAX_VALUE;
        }
    }

    /**
     * Write a {@code char} value using an unsafe memory write.
     */
    @Override
    public void set(Object object, char value) throws IllegalArgumentException {
        try {
            UnsafeMemory.unsafePutChar(object, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Compare two {@code char} values for equality.
     */
    @Override
    public boolean isEqual(Object a, Object b) {
        return getChar(a) == getChar(b);
    }

    /**
     * Copy the {@code char} value from {@code source} to {@code destination}.
     */
    @Override
    public void copy(Object source, Object destination) {
        set(destination, getChar(source));
    }
}
