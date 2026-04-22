/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.core.util.CoreDynamicEnum;

import java.util.List;

// REVIEW TASK CQDeprecationJavadoc: add a @deprecated Javadoc tag to DynamicEnum explaining the replacement and removal plan.
/**
 * Represents a dynamic enumeration which can either be a traditional {@code Enum} or a class
 * possessing a {@code String name} field. The interface extends both {@link CoreDynamicEnum} and
 * {@link Marshallable}, facilitating serialization and specific dynamic enumeration operations.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
@Deprecated(/* to be removed in 2027.x */)
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
