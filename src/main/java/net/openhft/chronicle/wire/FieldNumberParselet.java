//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.InvalidMarshallableException;

/**
 * Parses fields identified by a numeric ID.
 * This is primarily used with binary wire formats where fields can be
 * identified by numeric IDs instead of string names, for compactness and
 * efficiency. It is typically registered with a {@link WireParser}
 * (e.g., {@link VanillaWireParser}) as a fallback or for specific
 * field numbers.
 */
public interface FieldNumberParselet {

    /**
     * Invoked when a numeric field ID is parsed.
     *
     * @param methodId the numeric field ID read from the binary wire
     * @param wire     the {@link WireIn} from which the value should be read;
     *                 use {@code wire.getValueIn()} to access and deserialise the value
     * @throws InvalidMarshallableException if the value cannot be processed
     */
    void readOne(long methodId, WireIn wire) throws InvalidMarshallableException;
}
