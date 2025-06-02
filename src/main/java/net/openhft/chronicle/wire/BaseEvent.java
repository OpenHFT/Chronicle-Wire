/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

/**
 * Represents a timed event and extends {@link Marshallable} so that the event can
 * be serialised or de-serialised. This interface provides methods to manage that associated time
 * <p>
 * Typically used for events generated outside the system that carry a timestamp.
 * The time unit defaults to nanoseconds but can be overridden globally with the
 * {@code service.time.unit} system property.
 *
 * @param <E> type of the implementing event
 */
public interface BaseEvent<E extends BaseEvent<E>> extends Marshallable {
    /**
     * Returns the time at which the event which triggered this was generated (e.g. the time
     * an event generated externally to the system first entered the system).
     *
     * @return the time at which the event which triggered this was generated.
     */
    long eventTime();

    /**
     * Sets the timestamp for this event.
     *
     * @param eventTime the time to store, expressed in the service time unit
     *                  (see {@link ServicesTimestampLongConverter}).
     * @return this event instance for chaining
     * @throws UnsupportedOperationException if not overridden to store the value
     */
    default E eventTime(final long eventTime) {
        throw new UnsupportedOperationException();
    }

    /**
     * Convenience method that sets {@link #eventTime(long)} to the current time
     * via {@link ServicesTimestampLongConverter#currentTime()}.
     *
     * @return this event instance
     */
    default E eventTimeNow() {
        return eventTime(ServicesTimestampLongConverter.currentTime());
    }

    /**
     * Sets the event time to the current time via {@link #eventTimeNow()} if the
     * existing {@link #eventTime()} is not positive.
     *
     * @return this event instance
     */
    @SuppressWarnings("unchecked")
    default E updateEvent() {
        if (this.eventTime() <= 0)
            this.eventTimeNow();
        return (E) this;
    }
}
