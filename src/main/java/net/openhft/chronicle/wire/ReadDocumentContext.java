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
 * ReadDocumentContext is an interface that extends the DocumentContext interface.
 * It is tailored specifically for read operations within a document context.
 * The interface provides methods to initiate read operations and to set the read
 * limit and position within the context.
 */
public interface ReadDocumentContext extends DocumentContext {

    /**
     * Starts the read operation in the current DocumentContext.
     */
    void start();

    /**
     * Defines the read limit for the current ReadDocumentContext.
     * The read limit represents the maximum amount of data that can be read.
     *
     * @param readLimit The maximum amount of data (in bytes) to be read.
     */
    void closeReadLimit(long readLimit);

    /**
     * Sets the position at which reading begins in the current ReadDocumentContext.
     * The read position is a pointer to where the reading should start in the data stream.
     *
     * @param readPosition The position (in bytes) in the data stream where reading begins.
     */
    void closeReadPosition(long readPosition);
}
