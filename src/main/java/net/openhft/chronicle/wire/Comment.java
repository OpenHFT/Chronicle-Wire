//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Comment annotation represents a way to attach user-defined comments to elements
 * such as fields, parameters or types. This can be especially useful for documenting
 * the intention, usage or any other information about the annotated element at
 * runtime. Being retained at runtime, it can be inspected via reflection to influence
 * behaviour based on the comment's content. For example, a {@link WireOut}
 * implementation might choose to write the comment's value into the serialised output
 * if the wire format supports comments (for example YAML).
 */
@Retention(RetentionPolicy.RUNTIME)  // This annotation is available at runtime.
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})  // Specifies the kinds of elements this annotation can be applied to.
public @interface Comment {

    /**
     * Returns the comment content as a string.
     *
     * @return The comment content associated with the annotated element.
     */
    String value();
}
