/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Represents field information for double fields, extending the generic field information capabilities
 * provided by {@link UnsafeFieldInfo}. It offers direct memory access functionality to get and set
 * double values in objects, leveraging unsafe operations for enhanced performance.
 */
public final class DoubleFieldInfo extends UnsafeFieldInfo {

    /**
     * Constructs an instance of DoubleFieldInfo with the provided details about a double field.
     *
     * @param name        The name of the field.
     * @param type        The type of the field.
     * @param bracketType The bracket type associated with the field.
     * @param field       The field object representation.
     */
    public DoubleFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    @Override
    public double getDouble(Object object) {
        try {
            return UnsafeMemory.unsafeGetDouble(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(DoubleFieldInfo.class, e);
            return Double.NaN;
        }
    }

    @Override
    public void set(Object object, double value) throws IllegalArgumentException {
        try {
            UnsafeMemory.unsafePutDouble(object, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        return getDouble(a) == getDouble(b);
    }

    @Override
    public void copy(Object source, Object destination) {
        set(destination, getDouble(source));
    }
}
