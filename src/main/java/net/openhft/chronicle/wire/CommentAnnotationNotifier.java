/*
 * Copyright 2016-2020 chronicle.software
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

/**
 * This interface provides a mechanism for notifying
 * the presence of a preceding Comment annotation on a field.
 * Implementations can use this information to control
 * how the field is processed, typically to include the comment
 * in the serialized output.
 */
interface CommentAnnotationNotifier {

    /**
     * Notifies if there is a preceding Comment annotation.
     *
     * @param hasCommentAnnotation true if a preceding Comment annotation exists,
     *                             false otherwise
     */
    void hasPrecedingComment(boolean hasCommentAnnotation);
}
