/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueInStackTest extends WireTestCommon {

    @Test
    @DisplayName("pop throws when stack underflows below zero")
    void popThrowsOnUnderflow() {
        ValueInStack stack = new ValueInStack();
        stack.pop();
        assertEquals(-1, stack.level, "pop should decrement the level from zero to -1");
        assertThrows(IllegalStateException.class, stack::pop,
                "pop throws once the level is below zero");
    }
}
