/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.Mocker;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

/**
 * Represents a context that governs the interactions and state management for a document.
 * It provides methods to interrogate and manipulate the state of the associated document,
 * ensuring consistent and safe operations. This interface offers checks for metadata,
 * presence, and completion status of the document, as well as operations to manage the context's
 * lifecycle such as open, close, and reset.
 * <p>
 * Implementations must ensure proper handling of resources and consistency of the document state.
 */
public interface DocumentContext extends Closeable, SourceContext {
    /**
     * A no-op instance that ignores all calls; retained for legacy wiring.
     */
    @Deprecated(/* to be removed in 2027 */)
    DocumentContext NOOP = Mocker.ignored(DocumentContext.class);

    /**
     * Checks it the {@code DocumentContext} is metadata. If it is, {@code true} is
     * returned, otherwise {@code false}.
     *
     * @return true if the entry is metadata
     */
    boolean isMetaData();

    /**
     * Checks if the {@code DocumentContext} is present. If it is, {@code true} is returned,
     * otherwise {@code false}.
     *
     * @return true if the entry is present
     */
    boolean isPresent();

    /**
     * Determines if the {@code DocumentContext} is in an open state.
     * This method essentially checks the inverse of the completion status of the context.
     *
     * @return {@code true} if the context is open (i.e., the NOT_COMPLETE flag is set),
     * {@code false} otherwise.
     */
    default boolean isData() {
        return isPresent() && !isMetaData();
    }

    /**
     * Returns the {@link Wire} associated with the {@code Document}. It is possible that
     * {@code null} is returned, depending on the implementation.
     *
     * @return the {@link Wire} associated with the {@code Document}.
     */
    @Nullable
    Wire wire();

    /**
     * Indicates whether the NOT_COMPLETE flag is set for this document.
     *
     * @return whether the NOT_COMPLETE flag has been set.
     */
    boolean isNotComplete();

    /**
     * Indicates whether this document context remains open (not complete).
     *
     * @return {@code true} if the context is complete, {@code false} otherwise.
     */
    default boolean isOpen() {
        return isNotComplete();
    }

    /**
     * Invoked to signal an error condition in the current context.
     * This ensures that upon closing the context, any changes made during its lifecycle
     * are rolled back rather than committing a potentially erroneous state.
     */
    default void rollbackOnClose() {
    }

    /**
     * Call this if any incomplete message should be rolled back at this point, it it wasn't complete by now.
     */
    default void rollbackIfNotComplete() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    /**
     * Closes the {@code DocumentContext} and releases any held resources.
     * It is crucial to ensure that this method is invoked after the context's operations are completed
     * to prevent any potential resource leaks or data corruption.
     */
    @Override
    void close();

    /**
     * Cleans up the {@code DocumentContext} by invoking the close method, then discarding
     * any lingering state associated with it. This provides a way to ensure the context
     * is in a fresh state and free of any residual data or settings.
     */
    void reset();
}
