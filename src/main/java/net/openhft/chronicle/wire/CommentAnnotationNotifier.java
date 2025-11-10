//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

/**
 * <b>INTERNAL USE ONLY.</b> This interface is used by some {@link ValueOut}
 * implementations (for example for YAML) to signal to
 * {@link net.openhft.chronicle.wire.WireMarshaller.FieldAccess} that a
 * {@link Comment} annotation is present on a field. This allows the
 * {@code FieldAccess} to format the comment using the field's value after the
 * value itself has been written.
 */
interface CommentAnnotationNotifier {

    /**
     * Notifies the implementer whether a certain element has a preceding comment
     * annotation.
     *
     * @param hasCommentAnnotation {@code true} if a {@link Comment} annotation is
     *                             associated with the field about to be written,
     *                             {@code false} otherwise.
     */
    void hasPrecedingComment(boolean hasCommentAnnotation);
}
