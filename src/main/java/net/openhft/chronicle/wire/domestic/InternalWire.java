/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic;

/**
 * Internal hook used by Chronicle Wire implementations to adjust their state around header
 * processing.
 * <p>
 * Implementations may use {@link #forceNotInsideHeader()} when a nested operation must be treated
 * as outside any current header scope.
 */
public interface InternalWire {
    /**
     * Force the wire to treat subsequent operations as if no header is currently open.
     */
    void forceNotInsideHeader();
}
