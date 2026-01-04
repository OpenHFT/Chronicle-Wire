/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * This is the WriteAfterEOFException class extending IllegalStateException.
 * The exception is thrown when there's an attempt to write data after the End-Of-File (EOF) marker.
 * This is typically used to safeguard against improper file manipulation and to maintain data integrity.
 */
public class WriteAfterEOFException extends IllegalStateException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new instance of WriteAfterEOFException with a default error message.
     * The message indicates that writing after EOF is not permitted.
     */
    public WriteAfterEOFException() {
        super("You should not be able to write at EOF");
    }

    /**
     * Constructs a new instance of WriteAfterEOFException with a custom message.
     *
     * @param message the detail message for diagnostics
     */
    public WriteAfterEOFException(String message) {
        super(message);
    }
}
