/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic;

public interface InternalWire {
    void forceNotInsideHeader();

    /**
     * Replaces an end-of-data marker encountered from the current write position.
     * Callers must hold their storage write lock and validate the recovery target
     * before invoking this operation, then use ordinary strict header entry.
     *
     * @return the replaced marker's byte position, or {@code -1} if none was present
     */
    default long recoverFromEndOfData() {
        throw new UnsupportedOperationException("End-of-data recovery is not supported by this Wire");
    }
}
