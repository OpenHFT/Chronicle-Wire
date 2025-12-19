/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Import necessary libraries...

/**
 * Test suite for ignoring unknown datetime during deserialization.
 */
public class UnknownDatatimeTest extends WireTestCommon {

    /**
     * Tests if an unknown datetime can be ignored during deserialization.
     */
    @Test
    public void ignoreAnUnknownDateTime() {
        // Deserialize an instance of AClass from a string.
        // The string contains a datetime field 'eventTime' which is not expected to exist in AClass.
        AClass aClass = Marshallable.fromString("!" + AClass.class.getName() + " { eventTime: 2019-04-02T11:20:41.616653, id: 123456 }");

        // Assert that the 'id' field of the deserialized object has the expected value.
        // The absence of 'eventTime' in AClass should not cause any issues during deserialization.
        assertEquals(123456, aClass.id);
    }
}
