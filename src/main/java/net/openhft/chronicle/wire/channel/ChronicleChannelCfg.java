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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The ChronicleChannelCfg class is a configuration object for creating and configuring ChronicleChannel instances.
 * It offers a fluent API to customize parameters including the initiator flag, buffering mode, host parameters,
 * pauser mode and connection timeout settings.
 *
 * @param <C> the type of the implementing class that is derived from ChronicleChannelCfg
 */
public class ChronicleChannelCfg<C extends ChronicleChannelCfg<C>> extends SelfDescribingMarshallable {
    static {
        Handler.init();
    }

    private boolean initiator;
    private boolean buffered;
    private PauserMode pauser = PauserMode.yielding;

    @Deprecated(/* to be removed in x.27  - use net.openhft.chronicle.wire.channel.ChronicleChannelCfg.hostports instead */)
    private String hostname;


    @Deprecated(/* to be removed in x.27 - use net.openhft.chronicle.wire.channel.ChronicleChannelCfg.hostports instead */)
    private int port;

    private double connectionTimeoutSecs = 1.0;
    private final Set<HostPortCfg> hostports = new LinkedHashSet<>();

    /**
     * Returns the set of host ports configured for the ChronicleChannel.
     * If hostname and port are set, they will be included in the set.
     *
     * @return the set of host ports
     */
    public Set<HostPortCfg> hostPorts() {
        LinkedHashSet<HostPortCfg> result = new LinkedHashSet<>();
        if (hostname != null)
            result.add(new HostPortCfg(hostname, port));
        result.addAll(hostports);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Adds a hostname and port to the set of host ports.
     *
     * @param hostname the hostname
     * @param port     the port number
     * @return this configuration instance
     */
    public ChronicleChannelCfg<C> addHostnamePort(String hostname, int port) {
        hostports.add(new HostPortCfg(hostname, port));
        return this;
    }

    /**
     * Removes a hostname and port from the set of host ports.
     *
     * @param hostname the hostname
     * @param port     the port number
     */
    public void removeHostnamePort(String hostname, int port) {
        hostports.remove(new HostPortCfg(hostname, port));
    }

    /**
     * Sets the initiator flag.
     *
     * @param initiator the initiator flag
     * @return this configuration instance
     */
    public ChronicleChannelCfg<C> initiator(boolean initiator) {
        this.initiator = initiator;
        return this;
    }

    /**
     * Returns the initiator flag.
     *
     * @return the initiator flag
     */
    public boolean initiator() {
        return initiator;
    }

    /**
     * Returns the hostname.
     *
     * @return the hostname
     * @deprecated use {@link ChronicleChannelCfg#hostPorts()}
     */
    @Deprecated(/* to be removed in x.27 */)
    public String hostname() {
        return hostname;
    }

    /**
     * Sets the hostname for the connection.
     *
     * @param hostname the hostname
     * @return this configuration instance
     * @deprecated use {@link ChronicleChannelCfg#addHostnamePort(String, int)}
     */
    @Deprecated(/* to be removed in x.27 */)
    public C hostname(String hostname) {
        this.hostname = hostname;
        return (C) this;
    }

    /**
     * Returns the port number.
     *
     * @return the port number
     * @deprecated use {@link ChronicleChannelCfg#hostPorts()}
     */
    @Deprecated(/* to be removed in x.27 */)
    public int port() {
        return port;
    }

    /**
     * Sets the port number.
     *
     * @param port the port number
     * @return this configuration instance
     * @deprecated use {@link ChronicleChannelCfg#addHostnamePort(String, int)}
     */
    @Deprecated(/* to be removed in x.27 */)
    public C port(int port) {
        this.port = port;
        return (C) this;
    }

    /**
     * Returns the buffered flag.
     *
     * @return the buffered flag
     */
    public boolean buffered() {
        return buffered;
    }

    /**
     * Sets the buffering mode for the connection.
     *
     * @param buffered if true, enables buffering
     * @return this configuration instance for chaining method calls
     */
    public C buffered(boolean buffered) {
        this.buffered = buffered;
        return (C) this;
    }

    /**
     * Returns the PauserMode.
     *
     * @return the PauserMode
     */
    public PauserMode pauserMode() {
        return pauser;
    }

    /**
     * Sets the PauserMode to be used by the connection.
     *
     * @param pauser the PauserMode
     * @return this configuration instance for chaining method calls
     */
    public C pauserMode(PauserMode pauser) {
        this.pauser = pauser;
        return (C) this;
    }

    /**
     * Returns the connection timeout in seconds.
     * If not set or is set to a non-positive value,
     * it will return a default value based on the debug mode.
     *
     * @return the connection timeout in seconds
     */
    public double connectionTimeoutSecs() {
        if (connectionTimeoutSecs <= 0)
            return Jvm.isDebug() ? 120 : 10;
        return connectionTimeoutSecs;
    }

    /**
     * Sets the connection timeout in seconds.
     *
     * @param connectionTimeoutSecs the connection timeout in seconds
     * @return this configuration instance for chaining method calls
     */
    public C connectionTimeoutSecs(double connectionTimeoutSecs) {
        this.connectionTimeoutSecs = connectionTimeoutSecs;
        return (C) this;
    }

    /**
     * Clears all the host ports you have set up.
     */
    public void clearHostnamePort() {
        hostports.clear();
    }
}
