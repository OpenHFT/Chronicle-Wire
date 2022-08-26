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

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.converter.Base64;
import net.openhft.chronicle.wire.converter.NanoTime;

public class Lease extends SelfDescribingMarshallable {
    private String mac;
    private String type;
    private String name;
    private String notes = "";
    @NanoTime
    private long expiry;
    @Base64
    private long checkSum;

    public String mac() {
        return mac;
    }

    public Lease mac(String mac) {
        this.mac = mac;
        return this;
    }

    public String type() {
        return type;
    }

    public Lease type(String type) {
        this.type = type;
        return this;
    }

    public String name() {
        return name;
    }

    public Lease name(String name) {
        this.name = name;
        return this;
    }

    public String notes() {
        return notes;
    }

    public Lease notes(String notes) {
        this.notes = notes;
        return this;
    }

    public long expiry() {
        return expiry;
    }

    public Lease expiry(long expiry) {
        this.expiry = expiry;
        return this;
    }

    public long checkSum() {
        return checkSum;
    }

    public Lease checkSum(long checkSum) {
        this.checkSum = checkSum;
        return this;
    }
}
