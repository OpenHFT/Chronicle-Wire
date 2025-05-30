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
 * Extends {@link DocumentContext} with operations used when writing a document.
 * Implementations manage the lifecycle of the write and are expected to
 * finalise the document &ndash; for example by writing length prefixes &ndash;
 * when {@link #close()} is called.
 */
public interface WriteDocumentContext extends DocumentContext {

    /**
     * Prepares the context to write a new document or message.
     * If {@code isMetaData} is {@code true} the entry is marked as metadata,
     * otherwise it is data. Implementations typically call this from
     * {@link MarshallableOut} before any bytes are written to the wire.
     *
     * @param isMetaData {@code true} to mark the entry as metadata
     */
    void start(boolean isMetaData);

    /**
     * Indicates whether this write is part of a chained sequence.
     * When {@code true} closing the context may not finalise the document,
     * allowing further appends.
     *
     * @return {@code true} if the current write is chained
     */
    boolean chainedElement();

    /**
     * Sets whether this write forms part of a chained sequence.
     * When set to {@code true} additional writes may follow before the
     * document is finalised.
     *
     * @param isChained {@code true} to enable chaining
     */
    void chainedElement(boolean isChained);

    /**
     * Returns {@code true} if no data has been written since
     * {@link #start(boolean)} was invoked. Headers or markers added by the
     * implementation do not count as data.
     */
    boolean isEmpty();
}
