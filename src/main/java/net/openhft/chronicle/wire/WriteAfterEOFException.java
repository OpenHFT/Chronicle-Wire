/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Legacy exception retained for source and binary compatibility with callers
 * that handled strict end-of-data behaviour. Wire now warns and replaces an
 * end-of-data marker when a subsequent header is entered.
 */
public class WriteAfterEOFException extends IllegalStateException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs the legacy exception with its original message.
     */
    public WriteAfterEOFException() {
        super("You should not be able to write at EOF");
    }
}
