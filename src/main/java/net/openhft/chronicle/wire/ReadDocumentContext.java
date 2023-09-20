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
 * Represents a context for reading documents. This interface extends {@code DocumentContext}
 * and provides methods to manipulate the reading limits and positions within a document.
 *
 * @since 2023-09-11
 */
public interface ReadDocumentContext extends DocumentContext {

    /**
     * Initiates the start of reading within the context.
     */
    void start();

    /**
     * Sets the read limit for this {@code ReadDocumentContext}.
     * This defines the boundary or endpoint within the document up to which reading should occur.
     *
     * @param readLimit The long value representing the read limit.
     */
    void closeReadLimit(long readLimit);

    /**
     * Sets the read position for this {@code ReadDocumentContext}.
     * This defines the starting point within the document from which reading should begin.
     *
     * @param readPosition The long value representing the read position.
     */
    void closeReadPosition(long readPosition);
}
