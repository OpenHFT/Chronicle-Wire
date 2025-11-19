/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents a command for the cluster operations which also describes
 * itself when marshalled. This command is linked with a specific cycle and service.
 */
public class ClusterCommand extends SelfDescribingMarshallable {

    // The service ID in bytes format, initialized as an elastic byte buffer
    private final Bytes<?> serviceId = Bytes.allocateElasticOnHeap();
    // Represents the cycle associated with the command
    private long cycle;
    // The Service instance associated with the command
    private Service service;

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
     * @param cycle     The cycle for the command.
     * @param serviceId The service ID in CharSequence format.
     */
    public ClusterCommand(long cycle, CharSequence serviceId) {
        this.cycle = cycle;
        this.serviceId.clear().append(serviceId);
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
    private ClusterCommand serviceId(String serviceId) {
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
