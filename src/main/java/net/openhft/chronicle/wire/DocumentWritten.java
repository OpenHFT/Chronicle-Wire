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

/**
 * Entry point for obtaining a {@link DocumentContext} used when writing a document.
 * Implementations act as factories, returning contexts that must be closed to
 * release any associated locks or buffers.
 */
public interface DocumentWritten {
    /**
     * Starts a write operation.
     * <p>
     * Always close the returned context, preferably via try-with-resources,
     * otherwise locks may be held.
     *
     * @return a new {@link DocumentContext} for this write
     */
    DocumentContext writingDocument();

    /**
     * Starts a write operation with optional metadata.
     *
     * @param metaData include metadata if {@code true}
     * @return a new {@link DocumentContext} for this write
     * @throws UnrecoverableTimeoutException if an unrecoverable timeout occurs
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Obtains a {@link DocumentContext} for writing. Depending on the implementation
     * this may be a fresh context or a reusable instance and closing may be optional
     * for chained writes.
     *
     * @param metaData include metadata if {@code true}
     * @return a {@link DocumentContext} ready for use
     * @throws UnrecoverableTimeoutException if an unrecoverable timeout occurs
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;
}
