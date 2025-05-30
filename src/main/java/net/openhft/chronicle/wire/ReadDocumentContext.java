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
 * Represents a context for reading documents. This interface extends {@code DocumentContext}
 * and provides methods to manipulate the reading limits and positions within a document.
 * Implementations are responsible for detecting document boundaries and making the
 * content accessible through {@link #wire()}.
 */
public interface ReadDocumentContext extends DocumentContext {

    /**
     * Attempts to locate and prepare the next document or message for reading from the
     * underlying wire. After a successful call, {@link #isPresent()} indicates whether a
     * document was found and {@link #wire()} provides access to its content.
     */
    void start();

    /**
     * Sets the read limit for this {@code ReadDocumentContext}. This is typically used by
     * implementations during {@link #close()} to restore the previous read limit of the
     * underlying bytes if it was temporarily changed for reading the current document.
     * Not usually intended for external use.
     *
     * @param readLimit the read limit to restore
     */
    void closeReadLimit(long readLimit);

    /**
     * Sets the read position for this {@code ReadDocumentContext}. Implementations typically
     * call this during {@link #close()} to restore the previous read position of the underlying
     * {@link net.openhft.chronicle.bytes.Bytes} object if it was advanced past the current
     * document.
     *
     * @param readPosition the read position to restore
     */
    void closeReadPosition(long readPosition);
}
