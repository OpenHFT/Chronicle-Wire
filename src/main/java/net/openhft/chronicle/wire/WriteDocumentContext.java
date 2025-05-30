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

/**
 * A {@link DocumentContext} specialised for writing.  Implementations manage
 * the process of starting a new document, writing data and finalising the
 * length prefix when {@link #close()} is called.
 */
public interface WriteDocumentContext extends DocumentContext {

    /**
     * Prepare this context to write a new document.  Called by the owning
     * {@link MarshallableOut} before any data is written.
     *
     * @param metaData {@code true} if the document represents metadata
     */
    void start(boolean metaData);

    /**
     * Whether this write is part of a chained sequence of method calls where
     * the document is not finalised until the chain completes.
     */
    boolean chainedElement();

    /**
     * Mark this context as part of a chained write.
     */
    void chainedElement(boolean chainedElement);

    /**
     * Returns {@code true} if no actual data has been written since
     * {@link #start(boolean)} was invoked.
     */
    boolean isEmpty();
}
