/*
 * Copyright 2016-2025 chronicle.software
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

import net.openhft.chronicle.core.util.Mocker;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

/**
 * Represents a context that governs the interactions and state management for a document.
 * It is the primary mechanism for reading or writing individual messages within a {@link Wire}.
 * Typical usage is via a try-with-resources block where {@link #close()} commits or rolls back
 * the document. Implementations must ensure resources are released and the document state remains
 * consistent.
 */
public interface DocumentContext extends Closeable, SourceContext {
    /**
     * A no-operation implementation useful as a default or in tests where all
     * operations should be ignored. All method calls on this instance do nothing.
     */
    DocumentContext NOOP = Mocker.ignored(DocumentContext.class);

    /**
     * Checks if this context represents metadata. Metadata entries typically
     * contain control information or headers rather than main data.
     *
     * @return true if the entry is metadata
     */
    boolean isMetaData();

    /**
     * Indicates whether a document is currently available. For reading,
     * {@code true} means a valid message was found at the current position.
     * For writing, it is typically {@code true} while the context is open.
     *
     * @return true if a document is available
     */
    boolean isPresent();

    /**
     * Returns {@code true} if this context represents a present data document
     * (not metadata). This is equivalent to {@code isPresent() && !isMetaData()}.
     *
     * @return {@code true} if the entry is data
     */
    default boolean isData() {
        return isPresent() && !isMetaData();
    }

    /**
     * Returns the {@link Wire} associated with this document. Use the returned
     * wire to read or write data within the bounds of the current document.
     * Implementations may return {@code null} if the wire is not available.
     *
     * @return the associated wire, or {@code null}
     */
    @Nullable
    Wire wire();

    /**
     * Indicates whether the NOT_COMPLETE flag has been set. For writers this
     * means the document has not yet been fully committed. For readers it may
     * signal that an incomplete message was encountered.
     *
     * @return true if the document is not complete
     */
    boolean isNotComplete();

    /**
     * A convenience method equivalent to {@link #isNotComplete()}. Indicates if
     * the document context is currently open and active.
     *
     * @return true if the context is open
     */
    default boolean isOpen() {
        return isNotComplete();
    }

    /**
     * Invoked to signal an error condition. When called, closing the context
     * will roll back any changes instead of committing them. Typically used in a
     * catch block when writing fails.
     */
    default void rollbackOnClose() {
    }

    /**
     * For writing contexts, roll back the current document if it is still marked
     * as not complete. Implementations may throw
     * {@link UnsupportedOperationException} if rollback is not supported.
     */
    default void rollbackIfNotComplete() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    /**
     * Finalises the document and releases any held resources. In normal use this
     * method is invoked by a try-with-resources statement to guarantee it is
     * always called. Writers finalise length prefixes and make data visible;
     * readers release locks and advance the position.
     */
    @Override
    void close();

    /**
     * Resets the context to its initial state for possible reuse. This typically
     * closes the context and clears any internal state so it can be pooled or
     * reinitialised.
     */
    void reset();
}
