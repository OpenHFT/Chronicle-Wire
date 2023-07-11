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

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.core.util.WeakIdentityHashMap;
import net.openhft.chronicle.wire.QueryWire;
import net.openhft.chronicle.wire.channel.impl.SocketRegistry;
import net.openhft.chronicle.wire.channel.impl.internal.Handler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static net.openhft.chronicle.wire.WireType.YAML;

/**
 * This class represents a context for a Chronicle channel.
 * It contains all the parameters required to create and handle a channel including socket registry,
 * gateway and system context. This context can also hold other closeable resources and manage their lifecycle.
 */
public class ChronicleContext extends SimpleCloseable {
    static {
        // Initialize Handler at static context
        Handler.init();
    }

    // The set to manage all the closeable resources
    private transient final Set<Closeable> closeableSet =
            Collections.synchronizedSet(
                    Collections.newSetFromMap(
                            new WeakIdentityHashMap<>()));
    // URL for the Chronicle context
    private final String url;
    private String name;
    private transient URL _url;

    // Socket Registry for handling socket related operations
    private transient SocketRegistry socketRegistry;
    private boolean buffered;
    private boolean useAffinity;
    private ChronicleGatewayMain gateway;
    private SystemContext systemContext;
    private boolean privateSocketRegistry;

    /**
     * Protected constructor for creating a Chronicle context with the specified URL.
     *
     * @param url the URL for this context
     */
    protected ChronicleContext(String url) {
        this(url, null);
    }

    /**
     * Protected constructor for creating a Chronicle context with the specified URL and socket registry.
     *
     * @param url            the URL for this context
     * @param socketRegistry the socket registry for this context
     */
    protected ChronicleContext(String url, SocketRegistry socketRegistry) {
        this.url = url;
        this.socketRegistry = socketRegistry;
        init();
    }

    /**
     * Factory method for a new ChronicleContext
     *
     * @param url to connect to
     * @return the new ChronicleContext
     */
    public static ChronicleContext newContext(String url) {
        return new ChronicleContext(url);
    }

    /**
     * Obtain a URL from a String, loading any custom Handler as needed.
     *
     * @param spec to use for the URL
     * @return the URL
     * @throws IORuntimeException if malformed
     */
    public static URL urlFor(String spec) throws IORuntimeException {
        try {
            if (spec.startsWith("internal:"))
                return new URL(null, spec, new Handler());
            if (spec.startsWith("tcp:"))
                return new URL(null, spec, new net.openhft.chronicle.wire.channel.impl.tcp.Handler());
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * Obtain a URL from a String, loading any custom Handler as needed.
     *
     * @param spec to use for the URL
     * @return the URL
     * @throws IORuntimeException if malformed
     */
    public static List<URL> urlsFor(String spec) throws IORuntimeException {

        List<URL> result = new ArrayList<>();
        for (String s : spec.split(";")) {
            try {
                result.add(urlFor(s.trim()));
            } catch (Exception e) {
                Jvm.warn().on(ChronicleContext.class, e);
            }
        }
        return result;
    }

    /**
     * @return an AffinityLock appropriate for this context
     */
    public AffinityLock affinityLock() {
        return useAffinity()
                ? AffinityLock.acquireLock()
                : AffinityLock.acquireLock((String) null);
    }

    /**
     * @return whether affinity should be enabled.
     */
    public boolean useAffinity() {
        return useAffinity;
    }

    /**
     * Set whether affinity should be used if available.
     *
     * @param useAffinity if true
     * @return this
     */
    public ChronicleContext useAffinity(boolean useAffinity) {
        this.useAffinity = useAffinity;
        return this;
    }

    protected void init() {
        if (socketRegistry == null) {
            socketRegistry = new SocketRegistry();
            privateSocketRegistry = true;
        }
    }

    /**
     * Creates a new ChronicleChannelSupplier with this context and the given ChannelHandler.
     *
     * @param handler the ChannelHandler for the new ChronicleChannelSupplier
     * @return a new ChronicleChannelSupplier
     * @throws InvalidMarshallableException if there's a problem validating the header
     */
    public ChronicleChannelSupplier newChannelSupplier(ChannelHandler handler) throws InvalidMarshallableException {
        startServerIfNeeded();

        final ChronicleChannelSupplier connectionSupplier = new ChronicleChannelSupplier(this, handler);
        final String hostname = url().getHost();
        final int port = port();
        String query = url().getQuery();
        String connectionId = null;
        if (query != null) {
            QueryWire wire = new QueryWire(Bytes.from(query));
            connectionId = wire.read("sessionName").text();
        }

        connectionSupplier
                .protocol(url().getProtocol())
                .hostname(hostname == null || hostname.isEmpty() ? "localhost" : hostname)
                .port(port)
                .connectionId(connectionId)
                .buffered(buffered())
                .initiator(true);
        return connectionSupplier;
    }

    private void startServerIfNeeded() throws InvalidMarshallableException {
        if (url().getProtocol().equals("tcp") && "".equals(url().getHost())) {
            startNewGateway();
        }
    }

    /**
     * Starts a new Chronicle gateway if one doesn't already exist for this context.
     * The gateway is configured using the context's current parameters and added to the list of resources managed by the context.
     *
     * @throws InvalidMarshallableException if there's a problem creating or starting the gateway
     */
    public synchronized void startNewGateway() throws InvalidMarshallableException {
        // If gateway already exists, don't start a new one
        if (gateway != null)
            return;
        gateway = new ChronicleGatewayMain(url, socketRegistry, systemContext());
        gateway.name(name())
                .buffered(buffered())
                .useAffinity(useAffinity());
        try {
            // Add the gateway to the set of closeable resources
            addCloseable(gateway);
            // Start the gateway
            gateway.start();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * Add a closeable to be closed when this context is closed
     *
     * @param closeable to close
     */
    public void addCloseable(Closeable closeable) {
        closeableSet.add(closeable);
    }

    /**
     * Closes all the resources managed by this context.
     */
    protected void performClose() {
        Closeable.closeQuietly(closeableSet);
        closeableSet.clear();
        if (privateSocketRegistry)
            socketRegistry.close();
    }

    /**
     * @return the URL for this context
     */
    public URL url() {
        if (_url == null)
            _url = urlFor(url);

        return _url;
    }

    /**
     * @return should buffering be used
     */
    public boolean buffered() {
        return buffered;
    }

    /**
     * Sets whether buffering should be used.
     *
     * @param buffered if true.
     * @return this
     */
    public ChronicleContext buffered(boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    /**
     * @return the {@link SocketRegistry} for this context
     */
    public SocketRegistry socketRegistry() {
        return socketRegistry;
    }

    /**
     * Sets the system context for this context.
     *
     * @param systemContext the new system context
     * @throws InvalidMarshallableException if there's a problem setting the system context
     */
    public void systemContext(SystemContext systemContext) throws InvalidMarshallableException {
        this.systemContext = systemContext.deepCopy();
    }

    /**
     * This current {@link SystemContext} for this process or remote host
     *
     * @return the SystemContext
     */
    public SystemContext systemContext() {
        return systemContext == null ? SystemContext.INSTANCE.deepCopy() : systemContext;
    }

    /**
     * Sets the names space for the service. This can set the relative directory for example.
     *
     * @param name space of this context
     * @return this
     */
    public ChronicleContext name(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the name space of this context
     */
    public String name() {
        return name;
    }

    /**
     * Returns a new File instance for the given name space (sub-directory) within this context.
     *
     * @param name the name for the new File
     * @return a new File instance
     */
    public File toFile(String name) {
        if (this.name() == null)
            return new File(name);
        return new File(name(), name);
    }

    /**
     * @return the context in a YAML format
     */
    @Override
    public String toString() {
        return YAML.asString(this);
    }

    /**
     * @return the port for the gateway, if it exists, otherwise the port from the URL
     */
    public int port() {
        return gateway == null ? url().getPort() : gateway.port();
    }
}
