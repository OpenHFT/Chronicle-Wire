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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LeaseCache implements LeaseProvider {
    private final String mac;
    private final String path;
    private final LeaseProvider provider;
    private final List<Lease> freeLeases = new ArrayList<>();

    public LeaseCache(String mac, String path, LeaseProvider provider) throws IOException {
        this.mac = mac;
        this.path = path;
        this.provider = provider;
        final File pathFile = new File(path);
        long now = SystemTimeProvider.CLOCK.currentTimeNanos();
        if (pathFile.isDirectory()) {
            for (String name : pathFile.list()) {
                Lease lease = Marshallable.fromFile(Lease.class, path + "/" + name);
                if (lease.expiry() < now) {
                    new File(path, name).delete();
                } else if (mac.equals(lease.mac())) {
                    freeLeases.add(lease);
                }
            }
        }
    }

    @Override
    public List<Lease> requestLeases(String mac, String type, List<String> names) {
        List<Lease> leases = new ArrayList<>();
        List<String> unfoundName = new ArrayList<>();
        OUTER:
        for (String name : names) {
            for (Iterator<Lease> iterator = freeLeases.iterator(); iterator.hasNext(); ) {
                Lease lease = iterator.next();
                if (lease.mac().equals(mac)
                        && lease.type().equals(type)
                        && name.equals(lease.name())) {
                    iterator.remove();
                    leases.add(lease);
                    continue OUTER;
                }
            }
            unfoundName.add(name);
        }
        if (unfoundName.isEmpty())
            return leases;
        try {
            List<Lease> leases2 = requestLeases2(mac, type, unfoundName);
            leases.addAll(leases2);
            return leases;
        } catch (Exception e) {
            freeLeases.addAll(leases);
            throw e;
        }
    }

    /**
     * A convenience method for a single request
     *
     * @param type of the leased resource
     * @param name of the resource for that type
     * @return a lease
     * @throws IllegalStateException thrown if a lease isn't available
     */
    public Lease requestLease(String type, String name) throws IllegalStateException {
        for (Iterator<Lease> iterator = freeLeases.iterator(); iterator.hasNext(); ) {
            Lease freeLease = iterator.next();
            if (freeLease.type().equals(type)
                    && freeLease.name().equals(name)) {
                iterator.remove();
                return freeLease;
            }
        }

        final List<Lease> leases = requestLeases2(mac, type, Collections.singletonList(name));
        return leases.get(0);
    }

    private List<Lease> requestLeases2(String mac, String type, List<String> names) {
        final List<Lease> leases = provider.requestLeases(mac, type, names);
        int i = 0;
        OUTER:
        for (Lease lease : leases) {
            String type2 = lease.type();
            String name = lease.name();
            for (; i < 1000; i++) {
                String fileName = type2 + "-" + name + "-" + i;
                final File file = new File(path, fileName);
                if (file.exists())
                    continue;
                Wire wire = Wire.newYamlWireOnHeap();
                lease.writeMarshallable(wire);
                final Bytes<byte[]> bytes = (Bytes) wire.bytes();

                try {
                    BytesUtil.writeFile(file.getAbsolutePath(), bytes);
                    continue OUTER;

                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            throw new IllegalStateException("Cache full?");
        }
        return leases;
    }
}
