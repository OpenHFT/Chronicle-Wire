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

public interface DocumentWritten {
    /**
     * @return a context to use in a try-with-resource block
     */
    DocumentContext writingDocument();

    /**
     * Start a new DocumentContext, must always call close() when done.
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Start or reuse an existing a DocumentContext, optionally call close() when done.
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;
}
