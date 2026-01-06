/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodWireKeyTest extends WireTestCommon {

    @Test
    @DisplayName("wire key name returns the provided string value when present")
    void nameUsesProvidedValue() {
        MethodWireKey key = new MethodWireKey("alpha", 7);

        assertEquals("alpha", key.name(), "MethodWireKey.name should return the provided name");
        assertEquals(7, key.code(), "MethodWireKey.code should return the provided code");
    }

    @Test
    @DisplayName("name falls back to code value when name is null")
    void nameFallsBackToCode() {
        MethodWireKey key = new MethodWireKey(null, 42);

        assertEquals("42", key.name(), "MethodWireKey.name should format the code when name is null");
    }
}
