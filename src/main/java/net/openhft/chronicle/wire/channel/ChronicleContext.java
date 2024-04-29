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
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import static net.openhft.chronicle.wire.WireType.YAML;

/**
 * This class encapsulates the context for a Chronicle channel, including parameters necessary for creating
 * and managing a channel, such as the socket registry, gateway, and system context.
 * The context can also manage the lifecycle of other closeable resources.
 *
 * <p>The ChronicleContext provides methods to set important parameters such as the context's URL, whether
 * to use buffering and affinity, and the context's name (which can define the relative directory).
 * It also manages the lifecycle of closeable resources and provides the URL, buffering state, and the socket registry.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * String url = "tcp://:0";
 * try (ChronicleContext context = ChronicleContext.newContext(url)
 *     .name("target/server")
 *     .buffered(true)
 *     .useAffinity(true)) {
 *   ChronicleChannel channel = context.newChannelSupplier(new EchoHandler().buffered(false)).connectionTimeoutSecs(1).get();
 *   Says says = channel.methodWriter(Says.class);
 *   says.say("Hello World");
 *   StringBuilder eventType = new StringBuilder();
 *   String text = channel.readOne(eventType, String.class);
 *   assertEquals("say: Hello World", eventType + ": " + text);
 * </pre>
 */
@SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    private transient URL _url;

    // Socket Registry for handling socket related operations
    private transient SocketRegistry socketRegistry;
    private boolean buffered;
    private boolean useAffinity;
    private ChronicleGatewayMain gateway;
    private SystemContext systemContext;
    private boolean privateSocketRegistry;
    private Consumer<ChronicleChannel> closeCallback;

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
    @SuppressWarnings("this-escape")
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
     * Parses a URL from a string, initializing any custom handlers as necessary.
     *
     * <p>This method supports "internal:" and "tcp:" URL schemas in addition to the standard schemas.
     * If a URL starts with "internal:", a new Handler object is used as the URLStreamHandler.
     * If a URL starts with "tcp:", a new tcp.Handler object is used as the URLStreamHandler.
     * For other URL schemas, no custom URLStreamHandler is used.</p>
     *
     * @param spec the string to parse as a URL.
     * @return the URL parsed from the string.
     * @throws IORuntimeException if the string cannot be parsed as a URL.
     */
    @SuppressWarnings("deprecation")
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
     * @return an AffinityLock appropriate for this context
     * Acquires an AffinityLock instance based on the affinity usage status of the context. If affinity usage is enabled,
     * a lock is acquired without a specific tag. If affinity usage is disabled, a lock is acquired with a null tag.
     */
    public AffinityLock affinityLock() {
        return useAffinity()
                ? AffinityLock.acquireLock()
                : AffinityLock.acquireLock((String) null);
    }

    /**
     * Retrieves the status of affinity usage in this context.
     *
     * @return true if affinity usage is enabled, false otherwise.
     */
    public boolean useAffinity() {
        return useAffinity;
    }

    /**
     * Sets the status of affinity usage in this context.
     *
     * @param useAffinity a boolean flag indicating whether to enable affinity usage.
     * @return the current ChronicleContext instance, allowing for method chaining.
     */
    public ChronicleContext useAffinity(boolean useAffinity) {
        this.useAffinity = useAffinity;
        return this;
    }

    /**
     * Initializes the context, specifically it creates a new SocketRegistry instance if one is not already set.
     */
    protected void init() {
        if (socketRegistry == null) {
            socketRegistry = new SocketRegistry();
            privateSocketRegistry = true;
        }
    }

    /**
     * Constructs a new ChronicleChannelSupplier object using the provided ChannelHandler and the settings from this context.
     *
     * @param handler the ChannelHandler to be used by the new ChronicleChannelSupplier.
     * @return the newly created ChronicleChannelSupplier.
     * @throws InvalidMarshallableException if there is an error during the validation of the header.
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
                .addHostnamePort(hostname == null || hostname.isEmpty() ? "localhost" : hostname, port)
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
     * Starts a new instance of ChronicleGatewayMain if one is not already instantiated for this context.
     * Configures the new gateway with the context's current parameters and adds it to the list of
     * closeable resources managed by the context.
     *
     * @throws InvalidMarshallableException if there's an error during the creation or starting of the gateway.
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
     * Adds a Closeable resource to the context's internal set of resources.
     * This resource will be closed when the context itself is closed.
     *
     * @param closeable the Closeable resource to add.
     */
    public void addCloseable(Closeable closeable) {
        closeableSet.add(closeable);
    }

    /**
     * Closes all resources managed by this context in a quiet manner, i.e., without throwing any exceptions.
     * After closing, the set of Closeable resources is cleared. If the SocketRegistry was privately
     * created by this context, it is also closed.
     */
    protected void performClose() {
        Closeable.closeQuietly(closeableSet);
        closeableSet.clear();
        if (privateSocketRegistry)
            socketRegistry.close();
    }

    /**
     * Retrieves the URL of this context. If the URL hasn't been initialized yet, this method initializes it.
     *
     * @return the URL for this context.
     */
    public URL url() {
        if (_url == null)
            _url = urlFor(url);

        return _url;
    }

    /**
     * Indicates whether buffering should be used in this context.
     *
     * @return true if buffering should be used, false otherwise.
     */
    public boolean buffered() {
        return buffered;
    }

    /**
     * Sets the buffering preference for this context. If true, buffering will be used.
     *
     * @param buffered a boolean representing the preference for buffering.
     * @return this instance of ChronicleContext for method chaining.
     */
    public ChronicleContext buffered(boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    /**
     * Retrieves the SocketRegistry associated with this context.
     *
     * @return the {@link SocketRegistry} for this context.
     */
    public SocketRegistry socketRegistry() {
        return socketRegistry;
    }

    /**
     * Sets the system context for this context. The given system context will be deep copied to prevent
     * modifications to the original SystemContext from affecting this context.
     *
     * @param systemContext the new system context to be set.
     * @throws InvalidMarshallableException if there's an error while performing a deep copy of the system context.
     */
    public void systemContext(SystemContext systemContext) throws InvalidMarshallableException {
        this.systemContext = systemContext.deepCopy();
    }

    /**
     * Retrieves the current SystemContext for this process or remote host. If the system context is not set,
     * a new SystemContext instance is created, deep copied, and returned.
     *
     * @return the current {@link SystemContext} for this process or remote host.
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

    /**
     * @param closeCallback a callback that you can provide with will be called in the TCP Channel is closed
     */
    public void closeCallback(Consumer<ChronicleChannel> closeCallback) {
        this.closeCallback = closeCallback;
    }

    /**
     * @return this callback will be called in the TCP Channel is closed
     */
    public  Consumer<ChronicleChannel> closeCallback() {
        return this.closeCallback;
    }
}
