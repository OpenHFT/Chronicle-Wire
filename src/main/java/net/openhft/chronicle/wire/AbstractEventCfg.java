/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * The AbstractEventCfg class represents a generic configuration as an event.
 * It extends the AbstractMarshallableCfg and implements the Event interface.
 * This class provides methods to retrieve and set event properties such as ID, time, and service ID.
 * It also follows the builder pattern, allowing chained method calls.
 *
 * @param <E> The type parameter extending AbstractEventCfg
 */
@SuppressWarnings("unchecked")
public class AbstractEventCfg<E extends AbstractEventCfg<E>> extends AbstractMarshallableCfg implements Event<E> {

    // The unique identifier for the event (defaults to an empty string)
    private String eventId = "";

    // The timestamp of the event, typically in nanoseconds since epoch.
    // Converted via ServicesTimestampLongConverter when read from or
    // written to a textual wire format.
    @LongConversion(ServicesTimestampLongConverter.class)
    private long eventTime;

    // The service ID associated with the event.
    // Used for routing or identifying the source or target service.
    // Defaults to an empty string.
    private String serviceId = "";

    /**
     * Returns the unique identifier for this event.
     *
     * @return the current event ID
     */
    @NotNull
    @Override
    // to be removed in x.25
    public String eventId() {
        return eventId;
    }

    /**
     * Sets a new identifier for this event.
     *
     * @param eventId the unique identifier to assign; must not be null
     * @return this instance for chaining
     */
    @Override
    // to be removed in x.25
    public E eventId(@NotNull CharSequence eventId) {
        this.eventId = eventId.toString();
        return (E) this;
    }

    /**
     * Returns the timestamp associated with this event.
     *
     * @return the event time in the unit defined by
     *         {@link ServicesTimestampLongConverter}
     */
    @Override
    public long eventTime() {
        return eventTime;
    }

    /**
     * Sets the timestamp for this event.
     *
     * @param eventTime the new timestamp, typically in nanoseconds
     * @return this instance for chaining
     */
    @Override
    public E eventTime(long eventTime) {
        this.eventTime = eventTime;
        return (E) this;
    }

    /**
     * Updates the event time with the current system time.
     *
     * @return this instance for chaining
     */
    @Override
    public E eventTimeNow() {
        return this.eventTime(ServicesTimestampLongConverter.currentTime());
    }

    /**
     * Retrieves the service identifier associated with this event. This ID can
     * be used to route the event to a specific service or to identify the
     * service that originated it.
     *
     * @return the current service ID
     */
    @NotNull
    public String serviceId() {
        return serviceId;
    }

    /**
     * This method sets the provided service ID to the instance variable.
     *
     * @param serviceId the service ID to set; may be empty if not applicable
     * @return this instance for chaining
     */
    public E serviceId(String serviceId) {
        this.serviceId = serviceId;
        return (E) this;
    }

    /**
     * Checks if this event is intended for the given destination service.
     *
     * @param destServiceId The destination service ID to check against
     * @return true if the event is routed to the specified service, false otherwise
     */
    public boolean routedTo(String destServiceId) {
        return this.serviceId == null || this.serviceId().isEmpty() || this.serviceId().equals(destServiceId);
    }
}
