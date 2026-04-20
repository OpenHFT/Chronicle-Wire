/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Represents field information for long fields, extending the generic field information capabilities
 * provided by {@link UnsafeFieldInfo}. This class offers direct memory access functionality to get and set
 * long values in objects, leveraging unsafe operations for performance enhancement.
 */
public final class LongFieldInfo extends UnsafeFieldInfo {

    /**
     * Constructs an instance of LongFieldInfo with the provided details about a long field.
     *
     * @param name        The name of the field.
     * @param type        The type of the field.
     * @param bracketType The bracket type associated with the field.
     * @param field       The actual field representation.
     */
    public LongFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    @Override
    @SuppressWarnings("CSCallerCheckedBounds")
    public long getLong(Object object) {
        assert isInstance(object);
        try {
            return UnsafeMemory.unsafeGetLong(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(LongFieldInfo.class, e);
            return Long.MIN_VALUE;
        }
    }

    @Override
    @SuppressWarnings("CSCallerCheckedBounds")
    public void set(Object object, long value) throws IllegalArgumentException {
        assert isInstance(object);
        try {
            UnsafeMemory.unsafePutLong(object, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        return getLong(a) == getLong(b);
    }

    @Override
    public void copy(Object source, Object destination) {
        set(destination, getLong(source));
    }
}
