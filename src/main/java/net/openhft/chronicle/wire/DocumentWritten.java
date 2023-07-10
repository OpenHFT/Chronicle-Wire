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
 * The DocumentWritten interface provides methods for creating and acquiring DocumentContexts.
 * A DocumentContext provides a context for reading or writing document data. Methods in this
 * interface are used to initiate the writing process.
 */
public interface DocumentWritten {

    /**
     * Retrieves a new DocumentContext which should be used in a try-with-resources block
     * to ensure proper resource management.
     *
     * @return A new DocumentContext for writing operations.
     */
    DocumentContext writingDocument();

    /**
     * Creates a new DocumentContext for writing, with a flag to indicate whether the context
     * is metadata. After operations are completed, the close() method must be invoked to ensure
     * resources are released properly.
     *
     * @param metaData A boolean flag indicating whether the context is metadata.
     * @return A new DocumentContext for writing operations.
     * @throws UnrecoverableTimeoutException If there is a timeout while creating the DocumentContext.
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Attempts to acquire an existing DocumentContext for writing, or creates a new one if none exists.
     * The close() method can optionally be invoked after operations are done. This provides more flexibility
     * when dealing with DocumentContexts that might be used across different scopes.
     *
     * @param metaData A boolean flag indicating whether the context is metadata.
     * @return An existing or new DocumentContext for writing operations.
     * @throws UnrecoverableTimeoutException If there is a timeout while acquiring the DocumentContext.
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;
}
