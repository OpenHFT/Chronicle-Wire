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

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.channel.impl.internal.Handler;

public class ChronicleChannelCfg<C extends ChronicleChannelCfg<C>> extends SelfDescribingMarshallable {
    static {
        Handler.init();
    }

    private boolean initiator;
    private boolean buffered;
    private PauserMode pauser = PauserMode.yielding;
    private String hostname;
    private int port;

    private double connectionTimeoutSecs = 1.0;

    public ChronicleChannelCfg initiator(boolean initiator) {
        this.initiator = initiator;
        return this;
    }

    public boolean initiator() {
        return initiator;
    }

    public String hostname() {
        return hostname;
    }

    public C hostname(String hostname) {
        this.hostname = hostname;
        return (C) this;
    }

    public int port() {
        return port;
    }

    public C port(int port) {
        this.port = port;
        return (C) this;
    }

    public boolean buffered() {
        return buffered;
    }

    public C buffered(boolean buffered) {
        this.buffered = buffered;
        return (C) this;
    }

    public PauserMode pauserMode() {
        return pauser;
    }

    public C pauserMode(PauserMode pauser) {
        this.pauser = pauser;
        return (C) this;
    }

    public double connectionTimeoutSecs() {
        if (connectionTimeoutSecs <= 0)
            return Jvm.isDebug() ? 120 : 10;
        return connectionTimeoutSecs;
    }

    public C connectionTimeoutSecs(double connectionTimeoutSecs) {
        this.connectionTimeoutSecs = connectionTimeoutSecs;
        return (C) this;
    }
}
