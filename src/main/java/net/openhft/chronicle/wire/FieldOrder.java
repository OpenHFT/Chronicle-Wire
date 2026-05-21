/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the marshalling order for fields declared directly on the annotated class.
 * Field names are trimmed before validation and lookup.
 * Inherited fields keep the order declared by their own class.
 * <p>
 * The value must name every non-static, non-transient marshallable field declared
 * directly on the annotated class exactly once. It must not include inherited,
 * static, transient, blank, duplicate, or unknown field names.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FieldOrder {
    /**
     * Returns the ordered names of all directly declared marshallable fields.
     *
     * @return the field names in marshalling order
     */
    String[] value();
}
