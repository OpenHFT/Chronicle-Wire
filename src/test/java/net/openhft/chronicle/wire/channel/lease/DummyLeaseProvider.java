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

package net.openhft.chronicle.wire.channel.lease;

import net.openhft.chronicle.wire.converter.Base64;
import net.openhft.chronicle.wire.converter.NanoTime;

import java.util.ArrayList;
import java.util.List;

public class DummyLeaseProvider implements LeaseProvider {
    @NanoTime
    final long expiry;
    @Base64
    long sum = 0;

    public DummyLeaseProvider(String expiry) {
        this.expiry = NanoTime.INSTANCE.parse(expiry);
    }

    @Override
    public List<Lease> requestLeases(String mac, String type, List<String> names) {
        List<Lease> leases = new ArrayList<>();
        for (String name : names) {
            leases.add(new Lease().mac(mac).type(type).name(name).expiry(expiry).checkSum(++sum));
        }
        return leases;
    }
}
