/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic;

public interface InternalWire {
    void forceNotInsideHeader();

    /**
     * Replaces an end-of-data marker at the current write position with one compare-and-swap.
     * End-of-data markers are used with padded Wires. Callers must hold their storage write lock,
     * align and position the Wire at the validated recovery target, and ensure that write position
     * is correct before invoking this operation. This method does not search for an end-of-data
     * marker or adjust the write position. The compare-and-swap is a defensive invariant check, not
     * coordination between concurrent recovery writers; the storage layer must permit only one
     * active recovery coordinator for the target.
     *
     * @return {@code true} if the marker was replaced, or {@code false} if the current write
     * position did not contain an end-of-data marker
     */
    default boolean recoverFromEndOfData() {
        throw new UnsupportedOperationException("End-of-data recovery is not supported by this Wire");
    }
}
