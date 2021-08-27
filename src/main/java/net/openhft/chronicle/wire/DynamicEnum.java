package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.util.CoreDynamicEnum;

import java.util.List;

/**
 * Either the DynamicEnum must be an Enum or a class with a String name as a field.
 */
public interface DynamicEnum extends CoreDynamicEnum, Marshallable {

    /**
     * Uses a floating DynamicEnum to update the cached copy so every deserialization of the value from name() use have this information
     *
     * @param e template to use.
     */
    static <E extends DynamicEnum> void updateEnum(E e) {
        EnumCache<E> cache = EnumCache.of((Class<E>) e.getClass());
        E nums = cache.valueOf(e.name());
        List<FieldInfo> fieldInfos = e.$fieldInfos();
        for (FieldInfo fieldInfo : fieldInfos) {
            fieldInfo.set(nums, fieldInfo.get(e));
        }
    }
}
