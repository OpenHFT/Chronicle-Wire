/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

public interface Event<E extends Event<E>> extends Marshallable {

    /**
     * Returns a unique identifier attached to this event.
     *
     * @return a unique identifier attached to this event.
     */
    @NotNull
    @Deprecated(/* to be removed in x.25 */)
    default CharSequence eventId() {
        return "";
    }

    /**
     * Assigns a unique identifier to this event. The input identifier cannot be {@code null}.
     *
     * @param eventId unique identifier to assign to this event.
     * @return this
     */
    @Deprecated(/* to be removed in x.25 */)
    default E eventId(@NotNull final CharSequence eventId) {
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
