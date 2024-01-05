/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that can be serialized and deserialized through the {@link Marshallable} interface.
 * Each event has a unique identifier, a timestamp indicating when it was triggered, and other related metadata.
 * <p>
 * The generic parameter {@code E} allows for a self-referential type, facilitating builder-style method chaining.
 *
 * @param <E> the type of the event extending {@link Event}
 * @see Marshallable
 */
public interface Event<E extends Event<E>> extends Marshallable {

    /**
     * Retrieves the unique identifier associated with this event.
     *
     * @return The unique identifier for this event.
     * @deprecated This method is slated for removal in version x.25.
     */
    @NotNull
    @Deprecated(/* to be removed in x.25 */)
    default CharSequence eventId() {
        // Return an empty string as the default event ID
        return "";
    }

    /**
     * Sets the unique identifier for this event. The provided identifier must not be {@code null}.
     *
     * @param eventId The unique identifier to assign to this event.
     * @return The current instance of the event, facilitating method chaining.
     */
    @Deprecated(/* to be removed in x.25 */)
    default E eventId(@NotNull final CharSequence eventId) {
        // By default, the event identifier is unchanged and the current instance is returned.
        return (E) this;
    }

    /**
     * Returns the time at which the event which triggered this was generated (e.g. the time
     * an event generated externally to the system first entered the system).
     * <p>
     * By default, the time is represented in nanoseconds. System property 'service.time.unit'
     * can be changed in order to represent time in different units.
     *
     * @return the time at which the event which triggered this was generated.
     */
    long eventTime();

    /**
     * Sets the time at which the event which triggered this was generated (e.g. the time
     * an event generated externally to the system first entered the system).
     * <p>
     * The timestamp defaults to nanoseconds. However, the 'service.time.unit' system property
     * can be adjusted to represent time in alternative units.
     *
     * @param eventTime The timestamp to set for the triggering event.
     * @return The current instance of the event.
     * @throws UnsupportedOperationException if the operation is not supported.
     */
    default E eventTime(final long eventTime) {
        // By default, this method is unsupported and throws an exception.
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the time at which the event which triggered this was generated (e.g. the time
     * an event generated externally to the system first entered the system) to the
     * current time.
     * <p>
     * The timestamp defaults to nanoseconds, but the 'service.time.unit' system property
     * can be adjusted to use different time units.
     *
     * @return The current instance of the event.
     */
    default E eventTimeNow() {
        // Set the event time to the current system time
        return eventTime(ServicesTimestampLongConverter.currentTime());
    }

    default E updateEvent() {
        if (this.eventTime() <= 0)
            this.eventTimeNow();
        return (E) this;
    }

    /**
     * Updates event with new event name, updating event time to now if required.
     *
     * @param eventName name of the event
     */
    // TODO: x.25 remove eventName parameter
    default E updateEvent(final String eventName) {
        // Set the event ID to the given name if it's currently unset
        if (this.eventId().length() == 0)
            this.eventId(eventName);

        // Update the event time to the current system time if it's currently unset
        if (this.eventTime() <= 0)
            this.eventTimeNow();

        // Return the updated event instance
        return (E) this;
    }

    /**
     * Facilitates the transfer of details from one event to another.
     * Instead of copying properties directly between events, it is recommended to utilize this method.
     * Especially beneficial when considering future modifications, like the potential removal of eventId.
     *
     * @param from The source event from which details are to be copied.
     * @param to The target event to which details are to be applied.
     */
    static void copyEventDetails(Event<?> from, Event<?> to) {
        // Copy the eventId from the source event to the target event
        to.eventId(from.eventId());

        // Copy the eventTime from the source event to the target event
        to.eventTime(from.eventTime());
    }
}
