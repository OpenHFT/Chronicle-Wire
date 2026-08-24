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
 * hook is part of a public interface. Encountering EOF at a selected write position is an illegal
 * storage state unless a rolling output explicitly handles the signal by moving to another context.
 * A distinct exception preserves that signal without using a special header position that could be
 * ignored and then used as valid. Rolling outputs such as Chronicle Queue translate it into their
 * own lifecycle behaviour rather than expose it through ordinary append APIs.
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
