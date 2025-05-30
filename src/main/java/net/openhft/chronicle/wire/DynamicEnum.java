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
 * <b>Deprecated:</b> scheduled for removal in version x.28.
 * Represents an enumeration whose values may not be fixed at compile time.
 * Implementations can be either a traditional {@code Enum} or a class
 * with a {@code String name} field. The interface extends
 * {@link CoreDynamicEnum} and {@link Marshallable}.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
@Deprecated(/* to be removed in x.28 */)
public interface DynamicEnum extends CoreDynamicEnum, Marshallable {

    /**
     * <b>Deprecated.</b> Refreshes a cached dynamic enum using the supplied template.
     * It updates the cached instance with the same name so that subsequent lookups
     * via {@link EnumCache#valueOf(String)} return the latest data.
     *
     * @param e the template used to update the cached version
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
     * <b>Deprecated.</b> Dynamic enums are treated as immutable and this
     * default implementation always throws {@link UnsupportedOperationException}.
     */
    default void reset() {
        throw new UnsupportedOperationException();
    }
}
