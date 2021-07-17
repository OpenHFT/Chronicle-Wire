/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

public interface Event<E extends Event<E>> extends Marshallable {

    /**
     * Returns a unique identifier attached to this event.
     * @deprecated suggest to make #eventTime unique - Chronicle Services uses MappedUniqueTimeProvider
     *
     * @return a unique identifier attached to this event.
     */
    @Deprecated(/* to be removed in x.23 */)
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
    @Deprecated(/* to be removed in x.23 */)
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

    /**
     * Updates event with new event name, updating event time to now if required.
     *
     * @param eventName name of the event
     */
    // TODO: x.23 remove eventName parameter
    default void updateEvent(final String eventName) {
        if (this.eventId().length() == 0)
            this.eventId(eventName);

        if (this.eventTime() <= 0)
            this.eventTimeNow();
    }
}
