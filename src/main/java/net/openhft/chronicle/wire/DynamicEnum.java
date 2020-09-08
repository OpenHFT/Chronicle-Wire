package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.DynamicEnumPooled;
import net.openhft.chronicle.core.pool.EnumCache;

import java.util.List;

public interface DynamicEnum extends DynamicEnumPooled, Marshallable {
    /**
     * Uses afloating DynamicEnum to update the cached copy so every deserialization of the value from name() use have this information
     *
     * @param e template to use.
     */
    static <E extends Enum<E> & DynamicEnum> void updateEnum(E e) {
        EnumCache<E> cache = EnumCache.of((Class<E>) e.getClass());
        E nums = cache.valueOf(e.name());
        List<FieldInfo> fieldInfos = e.$fieldInfos();
        for (FieldInfo fieldInfo : fieldInfos) {
            fieldInfo.set(nums, fieldInfo.get(e));
        }
    }
}
