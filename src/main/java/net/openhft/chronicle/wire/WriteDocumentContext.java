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
 * WriteDocumentContext is an extension of the DocumentContext interface
 * that is specifically tailored for write operations. This interface provides
 * methods to start writing data, check and modify the chaining state of elements,
 * and verify if the context is empty.
 */
public interface WriteDocumentContext extends DocumentContext {

    /**
     * Starts the write operation in the current DocumentContext,
     * setting the metadata flag as specified.
     *
     * @param metaData A boolean flag indicating whether the context is metadata.
     */
    void start(boolean metaData);

    /**
     * Returns {@code true} if this {@code WriteDocumentContext} is a
     * chained element.
     *
     * @return {@code true} if this {@code WriteDocumentContext} is a
     * chained element.
     */
    boolean chainedElement();

    /**
     * Sets the current WriteDocumentContext's chain status.
     * If {@code chainedElement} is true, this WriteDocumentContext will be
     * marked as part of a chain of elements.
     *
     * @param chainedElement A boolean flag indicating whether the context is
     *                       part of a chain of elements.
     */
    void chainedElement(boolean chainedElement);

    /**
     * Checks if the current WriteDocumentContext is empty.
     *
     * @return {@code true} if the current WriteDocumentContext is empty,
     *         otherwise {@code false}.
     */
    boolean isEmpty();
}
