/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A test class for validating the default behaviour of AbstractCommonMarshallable.
 * This class extends the base test functionalities provided by WireTestCommon.
 */
class AbstractCommonMarshallableTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Tests the default behaviour of AbstractCommonMarshallable
     * to ensure it doesn't use self-describing messages by default.
     */
    @Test
    @DisplayName("Does not use self-describing messages by default")
    void doesNotUseSelfDescribingMessagesByDefault() {
        // Assert that a new instance of AbstractCommonMarshallable
        // doesn't use self-describing messages by default
        assertFalse(new AbstractCommonMarshallable() {
        }.usesSelfDescribingMessage(),
                "AbstractCommonMarshallable should disable self-describing messages by default");
    }
}
