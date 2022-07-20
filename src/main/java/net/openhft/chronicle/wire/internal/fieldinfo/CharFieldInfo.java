package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public final class CharFieldInfo extends UnsafeFieldInfo {

    public CharFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
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
