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
 * Represents an interface to notify whether a certain element has a preceding comment annotation.
 * Implementers can use this to handle scenarios where knowledge of preceding comments is required.
 *
 * @since 2023-09-14
 */
interface CommentAnnotationNotifier {

    /**
     * Notifies the implementer whether a certain element has a preceding comment annotation.
     *
     * @param hasCommentAnnotation A flag indicating if the element has a preceding comment.
     *                             True if it has, otherwise false.
     */
    void hasPrecedingComment(boolean hasCommentAnnotation);
}
