//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A test class for validating the behavior of AbstractCommonMarshallable.
 * This class extends the base test functionalities provided by WireTestCommon.
 */
class AbstractCommonMarshallableTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Tests the default behavior of AbstractCommonMarshallable
     * to ensure it doesn't use self-describing messages by default.
     */
    @Test
    void doesNotUseSelfDescribingMessagesByDefault() {
        // Assert that a new instance of AbstractCommonMarshallable
        // doesn't use self-describing messages by default
        assertFalse(new AbstractCommonMarshallable() {
        }.usesSelfDescribingMessage());
    }
}
