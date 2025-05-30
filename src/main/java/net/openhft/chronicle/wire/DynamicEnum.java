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

import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.util.CoreDynamicEnum;

import java.util.List;

/**
 * <b>Deprecated:</b> scheduled for removal in x.28.
 * <p>
 * Represents an enumeration whose values may be extended at runtime.  The
 * underlying type may be a traditional {@code enum} or any class with a
 * {@code String name} field.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
@Deprecated(/* to be removed in x.28 */)
public interface DynamicEnum extends CoreDynamicEnum, Marshallable {

    /**
     * Refresh an enum instance held in the internal {@link EnumCache} using the
     * supplied template {@code e}.
     */
    static <E extends DynamicEnum> void updateEnum(E e) {
        // Retrieve the enum cache corresponding to the class of the provided template
        EnumCache<E> cache = EnumCache.of((Class<E>) e.getClass());

        // Fetch the enum instance with the same name from the cache
        E nums = cache.valueOf(e.name());

        // Obtain field details of the provided template
        List<FieldInfo> fieldInfos = e.$fieldInfos();

        // Update each field in the cached enum instance using details from the template
        for (FieldInfo fieldInfo : fieldInfos) {
            fieldInfo.copy(e, nums);
        }
    }

    /**
     * Dynamic enums are treated as immutable and therefore cannot be reset.
     */
    default void reset() {
        throw new UnsupportedOperationException();
    }
}
