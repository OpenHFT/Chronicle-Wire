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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents a command for the cluster operations which also describes
 * itself when marshalled. This command is linked with a specific cycle and service.
 */
public class ClusterCommand extends SelfDescribingMarshallable {

    // Represents the cycle associated with the command
    private long cycle;

    // The Service instance associated with the command
    private Service service;

    // The service ID in bytes format, initialized as an elastic byte buffer
    private final Bytes<?> serviceId = Bytes.elasticByteBuffer();

    /**
     * Constructs a ClusterCommand with the given cycle and Service.
     *
     * @param cycle   The cycle for the command.
     * @param service The Service instance for the command.
     */
    public ClusterCommand(long cycle, Service service) {
        this.cycle = cycle;
        this.service = service;
        this.serviceId.clear().append(service.serviceId());
        //setUniqueTimeStampNow();
    }

    /**
     * Constructs a ClusterCommand with the given cycle and service ID.
     *
     * @param cycle      The cycle for the command.
     * @param serviceId  The service ID in CharSequence format.
     */
    public ClusterCommand(long cycle, CharSequence serviceId) {
        this.cycle = cycle;
        this.serviceId.clear().append(serviceId);
           // setUniqueTimeStampNow();
    }

    /**
     * Sets the cycle for this command.
     *
     * @param cycle The cycle to set.
     * @return The current ClusterCommand instance.
     */
    public ClusterCommand cycle(long cycle) {
        this.cycle = cycle;

        return this;
    }

    /**
     * Sets the Service for this command.
     *
     * @param service The Service instance to set.
     * @return The current ClusterCommand instance.
     */
    public ClusterCommand service(Service service) {
        this.service = service;

        if (service != null)
            return serviceId(service.serviceId());
        else
            return this;
    }

    /**
     * Sets the service ID for this command.
     *
     * @param serviceId The service ID to set.
     * @return The current ClusterCommand instance.
     */
    public ClusterCommand serviceId(String serviceId) {
        this.serviceId.clear().append(serviceId);

        return this;
    }

    /**
     * Retrieves the cycle associated with this command.
     *
     * @return The cycle of the command.
     */
    public long cycle() {
        return cycle;
    }

    /**
     * Retrieves the Service associated with this command.
     *
     * @return The Service instance.
     */
    public Service service() {
        return service;
    }

    /**
     * Retrieves the service ID associated with this command.
     *
     * @return The service ID in CharSequence format.
     */
    public CharSequence serviceId() {
        return serviceId;
    }
}
