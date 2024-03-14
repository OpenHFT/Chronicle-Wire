/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class AbstractEventCfg<E extends AbstractEventCfg<E>> extends AbstractMarshallableCfg implements Event<E> {

    // The unique identifier for the event
    private String eventId = "";

    // The timestamp indicating the time of the event
    @LongConversion(ServicesTimestampLongConverter.class)
    private long eventTime;

    // The service ID associated with the event
    private String serviceId = "";

    @NotNull
    @Override
    // to be removed in x.25
    public String eventId() {
        return eventId;
    }

    @Override
    // to be removed in x.25
    public E eventId(@NotNull CharSequence eventId) {
        this.eventId = eventId.toString();
        return (E) this;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public E eventTime(long eventTime) {
        this.eventTime = eventTime;
        return (E) this;
    }

    @Override
    public E eventTimeNow() {
        return this.eventTime(ServicesTimestampLongConverter.currentTime());
    }

    /**
     * This method retrieves the service ID associated with the event, used for event routing.
     * It returns the ID of the destination event.
     *
     * @return The current service ID of this object
     */
    @NotNull
    public String serviceId() {
        return serviceId;
    }

    /**
     * This method sets the provided service ID to the instance variable.
     * It returns the current instance, allowing chained method calls.
     *
     * @param serviceId The new service ID to be set
     * @return The current instance of AbstractEventCfg class
     */
    public E serviceId(String serviceId) {
        this.serviceId = serviceId;
        return (E) this;
    }

    /**
     * This method checks if the event is routed to a specific destination service.
     *
     * @param destServiceId The destination service ID to check against
     * @return true if the event is routed to the specified service, false otherwise
     */
    public boolean routedTo(String destServiceId) {
        return this.serviceId == null || this.serviceId().isEmpty() || this.serviceId().equals(destServiceId);
    }
}
