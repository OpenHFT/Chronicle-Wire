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

import org.jetbrains.annotations.NotNull;

/**
 * Represents an unrecoverable timeout exception, indicating that a certain operation
 * or request exceeded its allotted time and cannot be recovered or retried.
 * This exception extends the {@link IllegalStateException} to represent that the
 * system or application is in a state where the timeout cannot be handled in a
 * conventional manner.
 *
 * @since 2023-09-11
 */
public class UnrecoverableTimeoutException extends IllegalStateException {

    /**
     * Constructs a new UnrecoverableTimeoutException with the specified underlying
     * exception as the cause.
     * The message from the underlying exception is propagated to this exception.
     *
     * @param e The underlying exception that caused this timeout exception.
     */
    public UnrecoverableTimeoutException(@NotNull Exception e) {
        super(e.getMessage());
        initCause(e);
    }
}
