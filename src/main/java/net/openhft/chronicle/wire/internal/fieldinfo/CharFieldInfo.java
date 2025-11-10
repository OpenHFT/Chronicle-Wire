//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Represents field information for character fields, extending the generic field information capabilities
 * provided by {@link UnsafeFieldInfo}. It offers direct memory access functionality to get and set
 * character values in objects, leveraging unsafe operations for performance.
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

    @Override
    public char getChar(Object object) {
        try {
            return UnsafeMemory.unsafeGetChar(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(CharFieldInfo.class, e);
            return Character.MAX_VALUE;
        }
    }

    @Override
    public void set(Object object, char value) throws IllegalArgumentException {
        try {
            UnsafeMemory.unsafePutChar(object, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        return getChar(a) == getChar(b);
    }

    @Override
    public void copy(Object source, Object destination) {
        set(destination, getChar(source));
    }
}
