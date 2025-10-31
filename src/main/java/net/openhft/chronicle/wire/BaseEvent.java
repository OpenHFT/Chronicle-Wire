/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * can be changed in order to represent time in different units.
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
     * can be changed in order to represent time in different units.
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
