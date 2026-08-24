/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Thrown when an ordinary document write attempts to replace an end-of-data marker.
 * Validated recovery code uses a separate internal operation and therefore does not
 * weaken this contract for normal writers.
 * <p>
 * This type remains public because the internal-use {@link WireOut#enterHeader(long)} implementation
 * hook is part of a public interface. This direct checked {@link Exception} requires each storage
 * caller to handle a sealed position explicitly and cannot be absorbed by existing broad
 * {@code IOException} handling; a special return value could be ignored and then used as a header
 * position. Rolling outputs such as Chronicle Queue translate this signal into their own lifecycle
 * behaviour rather than expose it through ordinary append APIs.
 */
public class WriteAfterEOFException extends Exception {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs the exception with its established message.
     */
    public WriteAfterEOFException() {
        super("You should not be able to write at EOF");
    }
}
