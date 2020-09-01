/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

public interface Event<E extends Event<E>> extends Marshallable {
    /**
     * @return a unique ID for the source of the message.
     * It only needs to be useful for tracing the path of events through the system
     */
    @NotNull
    CharSequence eventId();

    /**
     * A system-assigned unique identifier for this event. It can be empty string.
     *
     * @param eventId unique id
     * @return this
     */
    default E eventId(@NotNull CharSequence eventId) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return The time at which the event which triggered this was generated
     * (e.g. the time an event generated externally to the system first entered the system).
     */
    long eventTime();

    default E eventTime(long eventTime) {
        throw new UnsupportedOperationException();
    }

    default E eventTimeNow() {
        return eventTime(ServicesTimestampLongConverter.currentTime());
    }

    /**
     * Update event with new event name, updating event time to now if required
     *
     * @param eventName name
     */
    default void updateEvent(String eventName) {
        if (this.eventId().length() == 0)
            this.eventId(eventName);

        if (this.eventTime() <= 0)
            this.eventTimeNow();
    }
}
