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

public class AbstractEventCfg<E extends AbstractEventCfg<E>> extends AbstractMarshallableCfg implements Event<E> {
    private String eventId = "";
    @LongConversion(ServicesTimestampLongConverter.class)
    private long eventTime;
    private String serviceId = "";

    @Override
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
     * Used for cfg event routing. serviceId of the destination event
     *
     * @return serviceId
     */
    @NotNull
    public String serviceId() {
        return serviceId;
    }

    public E serviceId(String serviceId) {
        this.serviceId = serviceId;
        return (E) this;
    }

    public boolean routedTo(String destServiceId) {
        return this.serviceId == null || this.serviceId().isEmpty() || this.serviceId().equals(destServiceId);
    }
}
