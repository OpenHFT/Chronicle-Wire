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

public class ChronicleContext extends SimpleCloseable {
    static {
        Handler.init();
    }

    private final Set<Closeable> closeableSet =
            Collections.synchronizedSet(
                    Collections.newSetFromMap(
                            new WeakIdentityHashMap<>()));
    private final String url;
    private String name;
    private transient URL _url;
    private transient SocketRegistry socketRegistry;
    private boolean buffered;
    private boolean useAffinity;
    private ChronicleGatewayMain gateway;
    private SystemContext systemContext;
    private boolean privateSocketRegistry;

    protected ChronicleContext(String url) {
        this(url, null);
    }

    protected ChronicleContext(String url, SocketRegistry socketRegistry) {
        this.url = url;
        this.socketRegistry = socketRegistry;
        init();
    }

    public static ChronicleContext newContext(String url) {
        return new ChronicleContext(url);
    }

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

    public AffinityLock affinityLock() {
        return useAffinity()
                ? AffinityLock.acquireLock()
                : AffinityLock.acquireLock((String) null);
    }

    public boolean useAffinity() {
        return useAffinity;
    }

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

    public ChronicleChannelSupplier newChannelSupplier(ChannelHandler handler) {
        startServerIfNeeded();

        final ChronicleChannelSupplier connectionSupplier = new ChronicleChannelSupplier(this, handler);
        final String hostname = url().getHost();
        final int port = gateway == null ? url().getPort() : gateway.port();
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

    private void startServerIfNeeded() {
        if (url().getProtocol().equals("tcp") && "".equals(url().getHost())) {
            startNewGateway();
        }
    }

    public synchronized void startNewGateway() {
        if (gateway != null)
            return;
        gateway = new ChronicleGatewayMain(url, socketRegistry, systemContext());
        gateway.name(name())
                .buffered(buffered())
                .useAffinity(useAffinity());
        try {
            addCloseable(gateway);
            gateway.start();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public void addCloseable(Closeable closeable) {
        closeableSet.add(closeable);
    }

    protected void performClose() {
        Closeable.closeQuietly(closeableSet);
        closeableSet.clear();
        if (privateSocketRegistry)
            socketRegistry.close();
    }

    public URL url() {
        if (_url == null)
            _url = urlFor(url);

        return _url;
    }

    public boolean buffered() {
        return buffered;
    }

    public ChronicleContext buffered(boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    public SocketRegistry socketRegistry() {
        return socketRegistry;
    }

    public void systemContext(SystemContext systemContext) {
        this.systemContext = systemContext.deepCopy();
    }

    public SystemContext systemContext() {
        return systemContext == null ? SystemContext.INSTANCE.deepCopy() : systemContext;
    }

    public ChronicleContext name(String name) {
        this.name = name;
        return this;
    }

    public String name() {
        return name;
    }

    public File toFile(String name) {
        if (this.name() == null)
            return new File(name);
        return new File(name(), name);
    }
}
