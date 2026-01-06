/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueInStateTest extends WireTestCommon {

    @Test
    @DisplayName("addUnexpected expands storage and preserves order")
    void addUnexpectedExpandsStorageAndPreservesOrder() {
        ValueInState state = new ValueInState();

        for (int i = 0; i < 12; i++) {
            state.addUnexpected(i * 10L);
        }

        assertEquals(12, state.unexpectedSize(), "ValueInState tracks unexpected count");
        assertEquals(0L, state.unexpected(0), "ValueInState stores first unexpected position");
        assertEquals(110L, state.unexpected(11), "ValueInState stores last unexpected position");
    }

    @Test
    @DisplayName("removeUnexpected shifts entries and handles tail removal")
    void removeUnexpectedShiftsEntriesAndHandlesTailRemoval() {
        ValueInState state = new ValueInState();
        state.addUnexpected(10L);
        state.addUnexpected(20L);
        state.addUnexpected(30L);

        state.removeUnexpected(1);
        assertEquals(2, state.unexpectedSize(), "ValueInState removes the selected entry");
        assertEquals(30L, state.unexpected(1), "ValueInState shifts entries after removal");

        state.removeUnexpected(1);
        assertEquals(1, state.unexpectedSize(), "ValueInState handles tail removal");
        assertEquals(10L, state.unexpected(0), "ValueInState preserves remaining entry");
    }

    @Test
    @DisplayName("reset clears saved position and unexpected count")
    void resetClearsSavedPositionAndUnexpectedCount() {
        ValueInState state = new ValueInState();
        state.savedPosition(17L);
        state.addUnexpected(3L);

        state.reset();

        assertEquals(0L, state.savedPosition(), "ValueInState reset clears saved position");
        assertEquals(0, state.unexpectedSize(), "ValueInState reset clears unexpected count");
    }
}
