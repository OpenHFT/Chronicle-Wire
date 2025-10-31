/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
