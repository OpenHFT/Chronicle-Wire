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
     * @return whether the NOT_COMPLETE flag has been set.
     */
    boolean isNotComplete();

    /**
     * @return {@code true} if the context is complete, {@code false} otherwise.
     */
    default boolean isOpen() {
        return isNotComplete();
    }

    /**
     * Returns the output context count associated with this document.
     * <p>
     * This allows DTOs with transient state to decide whether static context has already been
     * written to the current output context before writing an event that relies on it: remember
     * the count when the static context is written, and rewrite it when a document reports a
     * different count.
     * <p>
     * Contract:
     * <ul>
     *   <li>A valid count is always positive; {@code 0} is never returned with a real clock. (A
     *       Queue driven by a test time provider set at the epoch can produce roll cycle {@code 0};
     *       this only makes sense in tests and is not a production value.) A context whose count is
     *       unknown (no document present, or the context has been closed) returns a
     *       <em>negative</em> value.</li>
     *   <li>The value is bounded to the {@code int} range because supported context identifiers,
     *       including Queue roll cycles, are inherently {@code int}-valued.</li>
     *   <li>Compare counts with equality only. The value is an identifier, not an ordinal:
     *       Queue implementations return the roll cycle number for the document (which may have
     *       gaps), implementations without multiple output contexts return {@code 1}, and
     *       channel-like outputs should return the one-based connection count.</li>
     *   <li>For a given queue the value never goes backwards: a rolled-past cycle has an
     *       end-of-file marker and is never reused - a write with an earlier clock is refused
     *       rather than written to an earlier cycle.</li>
     *   <li>Implementations may throw {@link IllegalStateException} when the final context is not
     *       yet known; in particular a Queue configured for double buffering rejects this call
     *       (the target roll is selected only when the buffer is flushed), so progressive-DTO
     *       usage is not supported with double buffering.</li>
     *   <li>If a write fails after the caller has recorded the count, the recorded state may be
     *       ahead of the output; such failures are generally not retryable, so no attempt is made
     *       to make the count transactional with the write.</li>
     *   <li>Read the value from an open document on the writing thread; it is not thread-safe and
     *       is unspecified for a context object retained after close.</li>
     * </ul>
     *
     * @return the context count for this document, or a negative value if not known
     */
    default int contextCount() {
        return 1;
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
