package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public final class LongFieldInfo extends UnsafeFieldInfo {

    public LongFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    @Override
    public long getLong(Object object) {
        try {
            return UnsafeMemory.unsafeGetLong(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(LongFieldInfo.class, e);
            return Long.MIN_VALUE;
        }
    }

    @Override
    public void set(Object object, long value) throws IllegalArgumentException {
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
