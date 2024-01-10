/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

public interface TimedEvent<E extends TimedEvent<E>> extends Marshallable {
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
     * @return this
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
     * Updates event with new event name, updating event time to now if required.
     *
     * @param eventName name of the event
     */
    default E updateEvent() {
        if (this.eventTime() <= 0)
            this.eventTimeNow();
        return (E) this;
    }
}
