/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

public interface Event<E extends Event<E>> extends TimedEvent<E> {

    /**
     * Returns a unique identifier attached to this event.
     *
     * @return a unique identifier attached to this event.
     */
    @NotNull
    default CharSequence eventId() {
        return "";
    }

    /**
     * Assigns a unique identifier to this event. The input identifier cannot be {@code null}.
     *
     * @param eventId unique identifier to assign to this event.
     * @return this
     */
    default E eventId(@NotNull final CharSequence eventId) {
        return (E) this;
    }

    /**
     * Updates event with new event name, updating event time to now if required.
     *
     * @param eventName name of the event
     */
    default E updateEvent(final String eventName) {
        if (this.eventId().length() == 0)
            this.eventId(eventName);

        if (this.eventTime() <= 0)
            this.eventTimeNow();
        return (E) this;
    }

    /**
     * Rather than getting/setting from one event to the other directly, please use this method as
     * this will make removing eventId easier
     * @param from from
     * @param to to
     */
    static void copyEventDetails(Event<?> from, Event<?> to) {
        to.eventId(from.eventId());
        to.eventTime(from.eventTime());
    }
}
