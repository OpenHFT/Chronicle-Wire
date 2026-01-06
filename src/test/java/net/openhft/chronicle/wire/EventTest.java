/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest extends WireTestCommon {

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("updateEvent sets id and time when missing")
    void updateEventSetsIdAndTimeWhenMissing() {
        RecordingEvent event = new RecordingEvent();

        event.updateEvent("alpha");

        assertEquals("alpha", event.eventId(), "Event update should assign id when blank");
        assertTrue(event.eventTime() > 0, "Event update should set a positive time when missing");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("updateEvent preserves existing id and time")
    void updateEventPreservesExistingValues() {
        RecordingEvent event = new RecordingEvent().eventId("beta").eventTime(7);

        event.updateEvent("alpha");

        assertEquals("beta", event.eventId(), "Event update should keep the existing id value");
        assertEquals(7, event.eventTime(), "Event update should keep the existing time value");
    }

    @Test
    @DisplayName("copyEventDetails copies id and time from the source event")
    void copyEventDetailsMirrorsIdAndTime() {
        RecordingEvent source = new RecordingEvent().eventId("alpha").eventTime(17);
        RecordingEvent target = new RecordingEvent().eventId("beta").eventTime(3);

        Event.copyEventDetails(source, target);

        assertEquals("alpha", target.eventId(), "Event.copyEventDetails should copy the id value");
        assertEquals(17, target.eventTime(), "Event.copyEventDetails should copy the time value");
    }

    private static final class RecordingEvent implements Event<RecordingEvent> {
        private CharSequence id = "";
        private long time;

        @Override
        public long eventTime() {
            return time;
        }

        @Override
        public RecordingEvent eventTime(long eventTime) {
            this.time = eventTime;
            return this;
        }

        @Override
        public @NotNull CharSequence eventId() {
            return id == null ? "" : id;
        }

        @Override
        public RecordingEvent eventId(@NotNull CharSequence eventId) {
            this.id = eventId;
            return this;
        }
    }
}
