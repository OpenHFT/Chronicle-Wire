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

import net.openhft.chronicle.core.util.Mocker;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

/**
 * Represents a context for interacting with a single "document" (message) on a
 * {@link Wire}.  A document can either be data or metadata and may be in a
 * partially written state until {@link #close()} is invoked.
 * <p>
 * Implementations manage the lifecycle of the document and typically should be
 * used in a try-with-resources block:
 *
 * <pre>{@code
 *   try (DocumentContext dc = wire.writingDocument()) {
 *       // write fields
 *   }
 * }</pre>
 *
 * Closing the context finalises or rolls back the document depending on the
 * implementation.  The interface exposes flags to query whether the document is
 * metadata, present and/or complete.
 */
public interface DocumentContext extends Closeable, SourceContext {
    /**
     * A no-op implementation useful as a default or for testing.  All method
     * calls on this instance are ignored.
     */
    DocumentContext NOOP = Mocker.ignored(DocumentContext.class);

    /**
     * Indicates whether this document represents metadata as opposed to regular
     * user data.  Metadata documents are typically used for control messages or
     * headers within a wire.
     *
     * @return {@code true} if the document is metadata
     */
    boolean isMetaData();

    /**
     * For a reading context this returns {@code true} if a document was found
     * and is available for reading.  For a writing context it is usually
     * {@code true} while the context is open.
     *
     * @return {@code true} if the document is available
     */
    boolean isPresent();

    /**
     * Convenience method returning {@code true} if the document is present and
     * not metadata.  Equivalent to {@code isPresent() && !isMetaData()}.
     */
    default boolean isData() {
        return isPresent() && !isMetaData();
    }

    /**
     * Returns the {@link Wire} this document is read from or written to.  May be
     * {@code null} for specialised implementations.
     */
    @Nullable
    Wire wire();

    /**
     * Indicates whether the {@code NOT_COMPLETE} flag is currently set on the
     * document.  For writers this means the document length has not yet been
     * finalised.
     */
    boolean isNotComplete();

    /**
     * Convenience synonym for {@link #isNotComplete()}.
     */
    default boolean isOpen() {
        return isNotComplete();
    }

    /**
     * Invoked to signal an error condition in the current context so that when
     * {@link #close()} is called the document is rolled back instead of
     * committed.  Typically used in a {@code catch} block while writing.
     */
    default void rollbackOnClose() {
    }

    /**
     * Explicitly roll back the current document if it is still marked as
     * {@link #isNotComplete() not complete}.  Some implementations may throw
     * {@link UnsupportedOperationException} if rollback is not supported.
     */
    default void rollbackIfNotComplete() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    /**
     * Close the context and finalise the document.  Always call this method,
     * ideally via try-with-resources, to ensure resources are released and any
     * length prefixes are written.
     */
    @Override
    void close();

    /**
     * Reset the context to its initial state after closing.  Implementations may
     * reuse instances so this clears any internal state ready for another
     * document.
     */
    void reset();
}
