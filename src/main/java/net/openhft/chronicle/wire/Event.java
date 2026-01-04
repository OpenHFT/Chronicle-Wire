/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a business or system occurrence identified by an ID and time.
 * Extends {@link BaseEvent} for temporal handling and adds event identification.
 * <p>
 * NOTE: Only use this interface if the eventId is required as the eventTime is sufficient in most cases.
 * The eventId allows events to be routed or categorised beyond their timestamp.
 * <p>
 * The self-referential generic &lt;E extends Event&lt;E&gt;&gt; lets implementations return their own type for chaining.
 *
 * @param <E> the type of the implementing event class
 */
public interface Event<E extends Event<E>> extends BaseEvent<E> {

    /**
     * Identifier used for routing or logging.
     * Defaults to an empty string if none has been set.
     *
     * @return current identifier, or an empty string when unset
     */
    @NotNull
    default CharSequence eventId() {
        // Return an empty string as the default event ID
        return "";
    }

    /**
     * Assigns or replaces the identifier used for routing or logging.
     *
     * @param eventId unique identifier to assign, should not be null; an empty
     *                string can be used when no specific ID is required
     * @return this event instance for chaining
     */
    @SuppressWarnings("unchecked")
    default E eventId(@NotNull final CharSequence eventId) {
        // By default, the event identifier is unchanged and the current instance is returned.
        return (E) this;
    }

    /**
     * Updates the event ID and sets the time if needed.
     * The ID is set to {@code eventName} when the current ID is empty.
     * If {@link #eventTime()} is {@code <= 0} the time is updated via {@link #eventTimeNow()}.
     *
     * @param eventName name to assign when {@link #eventId()} is empty
     * @return this event instance after the update
     */
    @SuppressWarnings("unchecked")
    @Deprecated(/* to be removed in 2027 */)
    default E updateEvent(final String eventName) {
        // Set the event ID to the given name if it's currently unset
        if (this.eventId().length() == 0)
            this.eventId(eventName);

        // Update the event time to the current system time if it's currently unset
        if (this.eventTime() <= 0)
            this.eventTimeNow();
        return (E) this;
    }

    /**
     * Copies the eventId and eventTime from one event to another.
     * Prefer this helper over field access to keep a single point of change.
     *
     * @param from the source {@code Event}
     * @param to   the target {@code Event}
     */
    static void copyEventDetails(Event<?> from, Event<?> to) {
        to.eventId(from.eventId());
        to.eventTime(from.eventTime());
    }
}
