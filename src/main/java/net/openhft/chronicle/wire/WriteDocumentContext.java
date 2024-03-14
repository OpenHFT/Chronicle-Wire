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
 * This is the WriteDocumentContext interface extending DocumentContext.
 * It defines methods related to the writing context of a document.
 * The interface offers features to determine metadata status, check if elements are chained,
 * and discern if the document context is empty or not.
 */
public interface WriteDocumentContext extends DocumentContext {

    /**
     * Initializes the writing context with a specific metadata status.
     *
     * @param metaData A boolean value indicating if the context is metadata.
     */
    void start(boolean metaData);

    /**
     * Returns {@code true} if this {@code WriteDocumentContext} is a
     * chained element.
     *
     * @return {@code true} if this {@code WriteDocumentContext} is a
     * chained element; otherwise, {@code false}.
     */
    boolean chainedElement();

    /**
     * Marks this {@code WriteDocumentContext} as a chained element.
     *
     * @param chainedElement A boolean value indicating if the context
     * is a chained element.
     */
    void chainedElement(boolean chainedElement);

    /**
     * Checks if the writing context is empty.
     *
     * @return {@code true} if the context is empty; otherwise, {@code false}.
     */
    boolean isEmpty();
}
