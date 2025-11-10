//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

/**
 * Represents an abstract base class for binary marshallables that primarily deal with bytes.
 * By default, this class does not use self-describing messages.
 * <p>
 * This class extends the {@link AbstractCommonMarshallable} to provide common functionalities
 * shared among marshallables.
 */
public abstract class BytesInBinaryMarshallable extends AbstractCommonMarshallable {

    /**
     * Determines whether this marshallable uses self-describing messages.
     *
     * @return {@code false} indicating that this marshallable does not use self-describing messages by default.
     */
    @Override
    public boolean usesSelfDescribingMessage() {
        return false;
    }
}
