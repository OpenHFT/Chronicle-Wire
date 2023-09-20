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

package net.openhft.chronicle.wire.channel.impl;

/**
 * This is the HTTPDetectedException class.
 * It represents an exception condition where HTTP-related data or structures are encountered unexpectedly.
 * Given that this exception extends {@link InvalidProtocolException}, it denotes an issue where the HTTP protocol
 * was detected when it shouldn't have been or in a context where it was not expected.
 *
 * @since 2023-09-16
 */
public class HTTPDetectedException extends InvalidProtocolException {

    /**
     * Constructs a new HTTPDetectedException with the specified detail message.
     *
     * @param message The detail message, saved for later retrieval by the {@link Throwable#getMessage()} method.
     */
    public HTTPDetectedException(String message) {
        super(message);
    }
}
