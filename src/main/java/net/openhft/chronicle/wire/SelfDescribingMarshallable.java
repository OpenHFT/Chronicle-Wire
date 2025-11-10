//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

/**
 * Represents an abstraction of marshallable objects that are self-describing by default.
 * This class extends {@code AbstractCommonMarshallable} and ensures that instances
 * inherently use self-describing messages.
 */
public abstract class SelfDescribingMarshallable extends AbstractCommonMarshallable {

    /**
     * Always returns {@code true} as these marshallables emit type metadata.
     */
    @Override
    public boolean usesSelfDescribingMessage() {
        return true;
    }
}
