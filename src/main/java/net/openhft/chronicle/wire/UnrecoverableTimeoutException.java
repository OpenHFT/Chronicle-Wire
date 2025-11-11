/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an unrecoverable timeout exception, indicating that a certain operation
 * or request exceeded its allotted time and cannot be recovered or retried.
 * This exception extends the {@link IllegalStateException} to represent that the
 * system or application is in a state where the timeout cannot be handled in a
 * conventional manner.
 */
public class UnrecoverableTimeoutException extends IllegalStateException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new UnrecoverableTimeoutException with the specified underlying
     * exception as the cause.
     * The message from the underlying exception is propagated to this exception.
     *
     * @param e The underlying {@link Exception} that represents the original
     *          timeout or related error condition.
     */
    @SuppressWarnings("this-escape")
    public UnrecoverableTimeoutException(@NotNull Exception e) {
        super(e.getMessage());
        initCause(e);
    }
}
