package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public final class IntFieldInfo extends UnsafeFieldInfo {

    public IntFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
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
