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
 * Represents a dynamic enumeration which can either be a traditional {@code Enum} or a class
 * possessing a {@code String name} field. The interface extends both {@link CoreDynamicEnum} and
 * {@link Marshallable}, facilitating serialization and specific dynamic enumeration operations.
 */
@Deprecated(/* to be removed in x.27 */)
public interface DynamicEnum extends CoreDynamicEnum, Marshallable {

    /**
     * Refreshes the cached instance of a {@code DynamicEnum} based on the given template.
     * This ensures that every deserialization of the enum value from its {@code name()} method
     * is up-to-date with the most recent information.
     * <p>
     * Leveraging this method to update the cached enum details is essential for maintaining
     * data consistency, especially during frequent deserialization operations.
     *
     * @param e The {@code DynamicEnum} template used to refresh the cached version.
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
     * Not resettable, treat as immutable.
     */
    default void reset() {
        throw new UnsupportedOperationException();
    }
}
