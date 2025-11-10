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
 * Represents field information for integer fields, extending the generic field information capabilities
 * provided by {@link UnsafeFieldInfo}. It offers direct memory access functionality to get and set
 * integer values in objects, leveraging unsafe operations for enhanced performance.
 */
public final class IntFieldInfo extends UnsafeFieldInfo {

    /**
     * Constructs an instance of IntFieldInfo with the provided details about an integer field.
     *
     * @param name        The name of the field.
     * @param type        The type of the field.
     * @param bracketType The bracket type associated with the field.
     * @param field       The field object representation.
     */
    public IntFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    @Override
    public int getInt(Object object) {
        try {
            return UnsafeMemory.unsafeGetInt(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(IntFieldInfo.class, e);
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public void set(Object object, int value) throws IllegalArgumentException {
        try {
            UnsafeMemory.unsafePutInt(object, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        return getInt(a) == getInt(b);
    }

    @Override
    public void copy(Object source, Object destination) {
        set(destination, getInt(source));
    }
}
