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
 * The ChronicleChannelCfg class serves as a comprehensive configuration utility for constructing and tailoring ChronicleChannel instances.
 * Built with a fluent design, this class offers means to adjust a variety of parameters, namely the initiator status, buffering modality,
 * specific host configurations, pauser mode, and the length of connection timeout intervals.
 *
 * @param <C> a generic type which extends the ChronicleChannelCfg, enabling fluent method chaining
 */
public class ChronicleChannelCfg<C extends ChronicleChannelCfg<C>> extends SelfDescribingMarshallable {

    // Static block to initialize the Handler
    static {
        Handler.init();
    }

    // A flag indicating if the current configuration is set as the initiator
    private boolean initiator;

    // A flag indicating if buffering is enabled or disabled
    private boolean buffered;

    // Determines the mode of pausing; defaults to yielding mode
    private PauserMode pauser = PauserMode.yielding;

    // Deprecated; Represents the hostname for the configuration
    @Deprecated
    private String hostname;

    // Deprecated; Represents the port number for the configuration
    @Deprecated
    private int port;

    // Specifies the maximum time in seconds that the system will wait while trying to establish a connection
    private double connectionTimeoutSecs = 1.0;

    // A set of HostPort configurations for the ChronicleChannel
    private final Set<HostPortCfg> hostports = new LinkedHashSet<>();

    /**
     * Retrieves the set of host and port configurations set for the ChronicleChannel. It will also incorporate
     * any specified hostname and port if they are initialized.
     *
     * @return a read-only set of host and port configurations
     */
    public Set<HostPortCfg> hostPorts() {
        LinkedHashSet<HostPortCfg> result = new LinkedHashSet<>();
        // Check if the deprecated hostname is initialized and add to the set
        if (hostname != null)
            result.add(new HostPortCfg(hostname, port));
        // Incorporate the existing hostport configurations
        result.addAll(hostports);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Introduces a new hostname and port pair to the set of configurations.
     *
     * @param hostname the target hostname
     * @param port     the designated port number
     * @return the current configuration instance, supporting chained method calls
     */
    public ChronicleChannelCfg<C> addHostnamePort(String hostname, int port) {
        hostports.add(new HostPortCfg(hostname, port));
        return this;
    }

    /**
     * Excludes a specific hostname and port pair from the set of configurations.
     *
     * @param hostname the target hostname
     * @param port     the designated port number
     */
    public void removeHostnamePort(String hostname, int port) {
        hostports.remove(new HostPortCfg(hostname, port));
    }

    /**
     * Modifies the initiator flag for the current configuration.
     *
     * @param initiator the desired state for the initiator flag
     * @return the current configuration instance, supporting chained method calls
     */
    public ChronicleChannelCfg<C> initiator(boolean initiator) {
        this.initiator = initiator;
        return this;
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
     * Returns the hostname.
     *
     * @return the hostname
     * @deprecated use {@link ChronicleChannelCfg#hostPorts()}
     */
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
    public C port(int port) {
        this.port = port;
        return (C) this;
    }

    /**
     * Retrieves the status of the buffered flag.
     *
     * @return the current status of the buffered flag.
     */
    public boolean buffered() {
        return buffered;
    }

    /**
     * Modifies the buffering mode for the connection.
     * If set to true, buffering will be enabled; otherwise, it will be disabled.
     *
     * @param buffered a flag indicating whether buffering should be enabled
     * @return the current configuration instance, supporting chained method calls
     */
    public C buffered(boolean buffered) {
        this.buffered = buffered;
        return (C) this;
    }

    /**
     * Provides the currently set PauserMode.
     *
     * @return the active PauserMode.
     */
    public PauserMode pauserMode() {
        return pauser;
    }

    /**
     * Adjusts the PauserMode to be adopted by the connection.
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
     * Defines the connection timeout duration, measured in seconds.
     *
     * @param connectionTimeoutSecs the desired timeout duration in seconds
     * @return the current configuration instance, supporting chained method calls
     */
    public C connectionTimeoutSecs(double connectionTimeoutSecs) {
        this.connectionTimeoutSecs = connectionTimeoutSecs;
        return (C) this;
    }

    /**
     * Purges all the hostname and port configurations.
     * After invoking this method, the set of host and port configurations will be empty.
     */
    public void clearHostnamePort() {
        hostports.clear();
    }
}
