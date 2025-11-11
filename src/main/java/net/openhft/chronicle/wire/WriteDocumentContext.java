/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
