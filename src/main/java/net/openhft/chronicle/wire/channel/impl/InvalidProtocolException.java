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

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * This is the InvalidProtocolException class.
 * It signifies that there has been a protocol-related issue, especially when the expected protocol requirements
 * aren't met during data exchange or processing. This exception extends {@link IORuntimeException}, indicating
 * that the exception is related to I/O operations, but specifically points to protocol-related problems.
 */
public class InvalidProtocolException extends IORuntimeException {

    /**
     * Constructs a new InvalidProtocolException with the specified detail message.
     *
     * @param message The detail message, saved for later retrieval by the {@link Throwable#getMessage()} method.
     */
    public InvalidProtocolException(String message) {
        super(message);
    }
}
