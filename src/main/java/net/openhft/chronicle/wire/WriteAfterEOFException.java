/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Thrown by {@link WireOut#enterHeader(long)} when the selected write position holds an end-of-data marker.
 * The marker is a hard seal for ordinary writes. A rolling output catches this at its storage boundary and
 * moves to another context; Wire exposes no operation that replaces the marker.
 */
/// WriteOverEOFTest#ordinaryWriteRemainsSealedAtEOF demonstrates that this distinct exception remains
/// the repeatable hard-seal signal while Wire clears only transient header state.
public class WriteAfterEOFException extends IllegalStateException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs the exception with its established message.
     */
    public WriteAfterEOFException() {
        super("You should not be able to write at EOF");
    }
}
