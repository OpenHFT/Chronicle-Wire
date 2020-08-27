package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;


public class ClusterCommand extends SelfDescribingMarshallable {
    private long cycle;
    private Service service;
    private final Bytes serviceName = Bytes.elasticByteBuffer();

    public ClusterCommand(long cycle, Service service) {
        this.cycle = cycle;
        this.service = service;
        this.serviceName.clear().append(service.serviceName());
        //setUniqueTimeStampNow();
    }

    public ClusterCommand(long cycle, CharSequence serviceName) {
        this.cycle = cycle;
        this.serviceName.clear().append(serviceName);
    //    setUniqueTimeStampNow();
    }

    public ClusterCommand cycle(long cycle) {
        this.cycle = cycle;

        return this;
    }

    public ClusterCommand service(Service service) {
        this.service = service;

        if (service != null)
            return serviceName(service.serviceName());
        else
            return this;
    }

    public ClusterCommand serviceName(String serviceName) {
        this.serviceName.clear().append(serviceName);

        return this;
    }

    public long cycle() {
        return cycle;
    }

    public Service service() {
        return service;
    }

    public CharSequence serviceName() {
        return serviceName;
    }
}
