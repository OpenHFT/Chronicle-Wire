package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import net.openhft.chronicle.wire.VanillaFieldInfo;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

@SuppressWarnings("deprecation" /* The parent class will either be moved to internal or cease to exist in x.25 */)
class UnsafeFieldInfo extends VanillaFieldInfo {
    private static final long UNSET_OFFSET = Long.MAX_VALUE;
    private transient long offset = UNSET_OFFSET;

    public UnsafeFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    protected long getOffset() throws NoSuchFieldException {
        if (this.offset == UNSET_OFFSET) {
            offset = UnsafeMemory.unsafeObjectFieldOffset(getField());
        }
        return this.offset;
    }
}
