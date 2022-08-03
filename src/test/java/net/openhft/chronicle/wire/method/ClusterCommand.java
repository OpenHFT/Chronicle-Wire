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
