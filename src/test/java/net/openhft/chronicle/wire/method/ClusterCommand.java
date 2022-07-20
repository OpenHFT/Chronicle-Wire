package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class ClusterCommand extends SelfDescribingMarshallable {
    private long cycle;
    private Service service;
    private final Bytes<?> serviceId = Bytes.elasticByteBuffer();

    public ClusterCommand(long cycle, Service service) {
        this.cycle = cycle;
        this.service = service;
        this.serviceId.clear().append(service.serviceId());
        //setUniqueTimeStampNow();
    }

    public ClusterCommand(long cycle, CharSequence serviceId) {
        this.cycle = cycle;
        this.serviceId.clear().append(serviceId);
           // setUniqueTimeStampNow();
    }

    public ClusterCommand cycle(long cycle) {
        this.cycle = cycle;

        return this;
    }

    public ClusterCommand service(Service service) {
        this.service = service;

        if (service != null)
            return serviceId(service.serviceId());
        else
            return this;
    }

    public ClusterCommand serviceId(String serviceId) {
        this.serviceId.clear().append(serviceId);

        return this;
    }

    public long cycle() {
        return cycle;
    }

    public Service service() {
        return service;
    }

    public CharSequence serviceId() {
        return serviceId;
    }
}
