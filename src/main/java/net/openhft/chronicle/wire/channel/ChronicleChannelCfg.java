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
@SuppressWarnings("unchecked")
public class ChronicleChannelCfg<C extends ChronicleChannelCfg<C>> extends SelfDescribingMarshallable {
    static {
        Handler.init();
    }

    // A flag indicating if the current configuration is set as the initiator
    private boolean initiator;

    // A flag indicating if buffering is enabled or disabled
    private boolean buffered;

    // Determines the mode of pausing; defaults to yielding mode
    private PauserMode pauser = PauserMode.yielding;

    // Specifies the maximum time in seconds that the system will wait while trying to establish a connection
    private double connectionTimeoutSecs = 1.0;

    // A set of HostPort configurations for the ChronicleChannel
    private final Set<HostPortCfg> hostports = new LinkedHashSet<>();

    /**
     * Returns the set of host ports configured for the ChronicleChannel.
     * If hostname and port are set, they will be included in the set.
     *
     * @return the set of host ports
     */
    public Set<HostPortCfg> hostPorts() {
        return Collections.unmodifiableSet(hostports);
    }

    /**
     * Adds a hostname and port to the set of host ports.
     *
     * @param hostname the target hostname
     * @param port     the designated port number
     * @return the current configuration instance, supporting chained method calls
     */
    public C addHostnamePort(String hostname, int port) {
        hostports.add(new HostPortCfg(hostname, port));
        return (C) this;
    }

    /**
     * Removes a hostname and port from the set of host ports.
     *
     * @param hostname the target hostname
     * @param port     the designated port number
     */
    public void removeHostnamePort(String hostname, int port) {
        hostports.remove(new HostPortCfg(hostname, port));
    }

    /**
     * Sets the initiator flag.
     *
     * @param initiator the desired state for the initiator flag
     * @return the current configuration instance, supporting chained method calls
     */
    public C initiator(boolean initiator) {
        this.initiator = initiator;
        return (C) this;
    }

    /**
     * Provides the current status of the initiator flag.
     *
     * @return the present state of the initiator flag
     */
    public boolean initiator() {
        return initiator;
    }

    /**
     * Returns the buffered flag.
     *
     * @return the current status of the buffered flag.
     */
    public boolean buffered() {
        return buffered;
    }

    /**
     * Sets the buffering mode for the connection.
     *
     * @param buffered a flag indicating whether buffering should be enabled
     * @return the current configuration instance, supporting chained method calls
     */
    public C buffered(boolean buffered) {
        this.buffered = buffered;
        return (C) this;
    }

    /**
     * Returns the PauserMode.
     *
     * @return the active PauserMode.
     */
    public PauserMode pauserMode() {
        return pauser;
    }

    /**
     * Sets the PauserMode to be used by the connection.
     *
     * @param pauser the desired PauserMode
     * @return the current configuration instance, supporting chained method calls
     */
    public C pauserMode(PauserMode pauser) {
        this.pauser = pauser;
        return (C) this;
    }

    /**
     * Fetches the connection timeout duration, measured in seconds.
     * If the connectionTimeoutSecs is not explicitly set or holds a non-positive value,
     * the method will return a default value, the choice of which is influenced by the debug mode.
     *
     * @return the connection timeout duration in seconds
     */
    public double connectionTimeoutSecs() {
        // Default values based on the debug mode
        if (connectionTimeoutSecs <= 0)
            return Jvm.isDebug() ? 120 : 10;
        return connectionTimeoutSecs;
    }

    /**
     * Sets the connection timeout in seconds.
     *
     * @param connectionTimeoutSecs the desired timeout duration in seconds
     * @return the current configuration instance, supporting chained method calls
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
