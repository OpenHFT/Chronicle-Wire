/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseEventTest extends WireTestCommon {

    @Test
    @DisplayName("updateEvent sets an event time when time is missing")
    void updateEventSetsTimeWhenMissing() {
        TestEvent event = new TestEvent();
        event.time = 0;
        event.updateEvent();
        assertTrue(event.eventTime() > 0, "updateEvent should set a positive event time");
    }

    @Test
    @DisplayName("updateEvent keeps the existing event time when already set")
    void updateEventKeepsExistingTime() {
        TestEvent event = new TestEvent();
        event.time = 42;
        event.updateEvent();
        assertEquals(42, event.eventTime(), "updateEvent should not change a positive time");
    }

    private static final class TestEvent extends SelfDescribingMarshallable implements BaseEvent<TestEvent> {
        private long time;

        @Override
        public long eventTime() {
            return time;
        }

        @Override
        public TestEvent eventTime(long eventTime) {
            this.time = eventTime;
            return this;
        }
    }
}
