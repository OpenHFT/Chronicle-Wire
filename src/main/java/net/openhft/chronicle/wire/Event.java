/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * This interface defines the structure and behavior of an event within a system.
 * It extends {@link BaseEvent}, thereby inheriting methods related to time management,
 * and adds methods specific to event identification and manipulation.
 * <p>
 * NOTE: Only use this interface if the eventId is required as the eventTime is sufficient in most cases
 *
 * @param <E> The type of the implementing event class, following the self-referential generic pattern.
 */
public interface Event<E extends Event<E>> extends BaseEvent<E> {

    /**
     * Retrieves the unique identifier associated with this event.
     *
     * @return The unique identifier for this event.
     */
    @NotNull
    default CharSequence eventId() {
        // Return an empty string as the default event ID
        return "";
    }

    /**
     * Assigns an identifier to this event. The provided identifier must not be null.
     * This method can be used to explicitly set or change the event's identifier.
     *
     * @param eventId The unique identifier to assign to this event.
     * @return The current instance of the event, facilitating method chaining.
     */
    @SuppressWarnings("unchecked")
    default E eventId(@NotNull final CharSequence eventId) {
        // By default, the event identifier is unchanged and the current instance is returned.
        return (E) this;
    }

    /**
     * Updates the event with a new name, and if the event time is not already set,
     * updates the event time to the current system time. This method is useful for renaming
     * events and ensuring they have a valid timestamp.
     *
     * @param eventName The new name to be assigned to the event.
     * @return The current instance of the implementing class, with any necessary updates applied.
     */
    @SuppressWarnings("unchecked")
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
     * Copies essential details from one event to another. This method is preferred over direct
     * field access as it provides a more controlled way of transferring details between events,
     * and facilitates future changes to the event structure.
     *
     * @param from The source event from which details are copied.
     * @param to The target event to which details are copied.
     */
    static void copyEventDetails(Event<?> from, Event<?> to) {
        to.eventId(from.eventId());
        to.eventTime(from.eventTime());
    }
}
