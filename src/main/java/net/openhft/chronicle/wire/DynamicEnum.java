/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.util.CoreDynamicEnum;

import java.util.List;

/**
 * Either the DynamicEnum must be an Enum or a class with a String name as a field.
 */
public interface DynamicEnum<E extends DynamicEnum<E>> extends CoreDynamicEnum<E>, Marshallable {

    /**
     * Uses a floating DynamicEnum to update the cached copy so every deserialization of the value from name() use have this information
     *
     * @param e template to use.
     */
    static <E extends DynamicEnum<E>> void updateEnum(E e) {
        EnumCache<E> cache = EnumCache.of(Jvm.uncheckedCast(e.getClass()));
        E nums = cache.valueOf(e.name());
        List<FieldInfo> fieldInfos = e.$fieldInfos();
        for (FieldInfo fieldInfo : fieldInfos) {
            fieldInfo.copy(e, nums);
        }
    }

    /**
     * Not resettable, treat as immutable.
     */
    default void reset() {
        throw new UnsupportedOperationException();
    }
}
