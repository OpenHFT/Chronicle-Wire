//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.utils;

/**
 * Enumerates the possible outcomes of a single read attempt by a
 * {@link net.openhft.chronicle.bytes.MethodReader}, particularly for
 * generated readers such as {@link net.openhft.chronicle.wire.AbstractGeneratedMethodReader}.
 */
public enum MethodReaderStatus {
    /**
     * Indicates that no message or event was found or processed in the current
     * read attempt (for example end of document or no data available).
     */
    EMPTY,
    /**
     * Indicates that a {@link net.openhft.chronicle.wire.MessageHistory} event
     * was read and processed.
     */
    HISTORY,
    /**
     * Indicates that a known method or event was successfully read,
     * deserialized and dispatched to a handler.
     */
    KNOWN,
    /**
     * Indicates that an unknown method or event was encountered. The default
     * parselet may have skipped it or an error might have been logged.
     */
    UNKNOWN
}
