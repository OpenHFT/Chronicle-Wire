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
 * A {@link DocumentContext} specialised for reading.  Implementations detect
 * the next document boundary and expose the document's bytes via
 * {@link #wire()}.
 */
public interface ReadDocumentContext extends DocumentContext {

    /**
     * Locate and prepare the next document in the underlying {@link Wire}.  On
     * return {@link #isPresent()} indicates if a document was found.
     */
    void start();

    /**
     * Restore the read limit of the underlying bytes when closing.  Generally
     * used by the implementation rather than user code.
     */
    void closeReadLimit(long readLimit);

    /**
     * Restore the read position of the underlying bytes on close.
     */
    void closeReadPosition(long readPosition);
}
