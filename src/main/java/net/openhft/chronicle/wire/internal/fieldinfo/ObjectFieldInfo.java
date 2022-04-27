package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Objects;

public final class ObjectFieldInfo extends UnsafeFieldInfo {

    public ObjectFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    @Override
    public @Nullable Object get(Object object) {
        try {
            return UnsafeMemory.unsafeGetObject(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(ObjectFieldInfo.class, e);
            return null;
        }
    }

    @Override
    public void set(Object object, Object value) throws IllegalArgumentException {
        Object value2 = ObjectUtils.convertTo(type, value);
        try {
            UnsafeMemory.unsafePutObject(object, getOffset(), value2);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        return Objects.deepEquals(get(a), get(b));
    }

    @Override
    public void copy(Object source, Object destination) {
        set(destination, get(source));
    }
}
