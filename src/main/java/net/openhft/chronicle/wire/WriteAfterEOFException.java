/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Thrown when an ordinary document write attempts to replace an end-of-data marker.
 * Validated recovery code uses a separate internal operation and therefore does not
 * weaken this contract for normal writers.
 */
public class WriteAfterEOFException extends IllegalStateException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs the exception with its established message.
     */
    public WriteAfterEOFException() {
        super("You should not be able to write at EOF");
    }
}
