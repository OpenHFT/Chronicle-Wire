/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

/**
 * This interface represents a timed event, providing methods to manage and access the time associated with the event.
 * It is typically used for events that have a specific timestamp, such as those generated externally and captured by the system.
 * <p>
 * The event time is generally represented in nanoseconds. However, this can be adjusted if needed through the system property 'service.time.unit'.
 *
 * @param <E> The type of the implementing event class
 */
public interface BaseEvent<E extends BaseEvent<E>> extends Marshallable {
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
     * By default, the time is represented in nanoseconds. System property 'service.time.unit'
     * can be changed in order to represent time in different units.
     *
     * @param eventTime The timestamp to set for the event, in the configured time unit.
     * @return The current instance of the implementing class.
     * @throws UnsupportedOperationException if the method is not overridden.
     */
    default E eventTime(final long eventTime) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the time at which the event which triggered this was generated (e.g. the time
     * an event generated externally to the system first entered the system) to the
     * current time.
     * <p>
     * By default, the time is represented in nanoseconds. System property 'service.time.unit'
     * can be changed in order to represent time in different units.
     *
     * @return this
     */
    default E eventTimeNow() {
        return eventTime(ServicesTimestampLongConverter.currentTime());
    }

    /**
     * Updates event's time with the current time if it hasn't been set.
     */
    @SuppressWarnings("unchecked")
    default E updateEvent() {
        if (this.eventTime() <= 0)
            this.eventTimeNow();
        return (E) this;
    }
}
