/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Represents a context for reading documents. This interface extends {@code DocumentContext}
 * and provides methods to manipulate the reading limits and positions within a document.
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
