/*
 * Copyright 2016-2022 chronicle.software
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
 * Represents an entity that supports writing documents. The interface provides methods
 * to initiate and manage the context in which documents are written. It is crucial
 * to manage the lifecycle of the {@link DocumentContext} correctly, either by using
 * try-with-resources or explicitly invoking the close() method.
 *
 * @since 2023-09-14
 */
public interface DocumentWritten {
    /**
     * Creates a new {@link DocumentContext} for writing a document.
     * This context is designed for use within a try-with-resource block to ensure
     * proper resource management.
     *
     * @return A fresh {@link DocumentContext} for writing.
     */
    DocumentContext writingDocument();

    /**
     * Initiates a new {@link DocumentContext} for writing, with an option to include
     * metadata. It is imperative to always invoke the close() method on the context
     * after completing the write operation.
     *
     * @param metaData A boolean indicating if metadata should be included during writing.
     * @return A fresh {@link DocumentContext} tailored to the metadata preference.
     * @throws UnrecoverableTimeoutException If the operation times out in an unrecoverable manner.
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Obtains a {@link DocumentContext} for writing. This method either initiates a new context
     * or reuses an existing one. Depending on the use case, calling the close() method
     * on the context might be optional.
     *
     * @param metaData A boolean indicating if metadata should be included during writing.
     * @return An existing or new {@link DocumentContext} tailored to the metadata preference.
     * @throws UnrecoverableTimeoutException If the operation times out in an unrecoverable manner.
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;
}
