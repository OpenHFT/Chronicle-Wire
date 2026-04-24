/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
     * Sets the time at which the event which triggered this was generated (e.g. the time
     * an event generated externally to the system first entered the system).
     * <p>
     * By default, the time is represented in nanoseconds. System property 'service.time.unit'
     * can be changed to represent time in different units.
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
     * Sets the time at which the event which triggered this was generated (e.g. the time
     * an event generated externally to the system first entered the system) to the
     * current time.
     * <p>
     * By default, the time is represented in nanoseconds. System property 'service.time.unit'
     * can be changed to represent time in different units.
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
