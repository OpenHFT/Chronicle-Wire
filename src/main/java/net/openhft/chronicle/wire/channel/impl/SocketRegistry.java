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

package net.openhft.chronicle.wire.channel.impl;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.util.WeakIdentityHashMap;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

// TODO This is a simplified net.openhft.chronicle.network.TCPRegistry to cover the use cases tested
/**
 * This is the SocketRegistry class.
 * It provides a simplified representation of the net.openhft.chronicle.network.TCPRegistry, designed specifically for the tested use cases.
 * The primary functions of this class are to manage and provide server and client socket channels based on the given inputs.
 * It also maintains a collection of all opened closeable resources, ensuring they are closed properly upon shutting down the registry.
 */
public class SocketRegistry extends AbstractCloseable {

    // Mapping from a description to the corresponding server socket channel
    private final Map<String, ServerSocketChannel> descToServerSocketChannelMap = new ConcurrentSkipListMap<>();

    // A set of closeable resources, maintained to ensure proper cleanup
    private final Set<Closeable> closeableSet =
            Collections.synchronizedSet(
                    Collections.newSetFromMap(
                            new WeakIdentityHashMap<>()));

    // Not thread-safe but deemed acceptable for the current use case
    int lastHost = 0;

    /**
     * Acquires a server socket channel using the provided URL.
     *
     * @param url The URL containing the hostname and port details.
     * @return An opened {@link ServerSocketChannel}.
     * @throws IOException If there's an error while opening the server socket channel.
     */
    public ServerSocketChannel acquireServerSocketChannel(URL url) throws IOException {
        return acquireServerSocketChannel(url.getHost(), url.getPort());
    }

    /**
     * Acquires or reuses an existing server socket channel based on the provided hostname and port.
     *
     * @param hostname The hostname for the server socket channel.
     * @param port The port for the server socket channel.
     * @return An opened {@link ServerSocketChannel}.
     * @throws IOException If there's an error while opening the server socket channel.
     */
    private synchronized ServerSocketChannel acquireServerSocketChannel(String hostname, int port) throws IOException {
        // Generate the description
        String description = hostname == null || hostname.isEmpty()
                ? "port" + port
                : port > 0
                ? hostname + ':' + port
                : hostname;
        ServerSocketChannel ssc = descToServerSocketChannelMap.get(description);
        if (ssc != null && ssc.isOpen())
            return ssc;
        ssc = ServerSocketChannel.open();
        addCloseable(ssc);
        ssc.bind(new InetSocketAddress(port));
        descToServerSocketChannelMap.put(description, ssc);
        return ssc;
    }

    /**
     * Creates and opens a client socket channel based on the provided hostname and port.
     * If the hostname contains multiple comma-separated entries, it rotates between them.
     *
     * @param hostname The hostname for the client socket channel.
     * @param port The port for the client socket channel.
     * @return An opened {@link SocketChannel}.
     * @throws IOException If there's an error while opening the socket channel.
     */
    public synchronized SocketChannel createSocketChannel(String hostname, int port) throws IOException {
        // Handle multiple hostnames scenario
        if (hostname.contains(",")) {
            String[] hostnames = hostname.split(",");
            int lastHost = this.lastHost;
            if (lastHost >= hostnames.length)
                lastHost = 0;
            hostname = hostnames[lastHost];
            this.lastHost = lastHost + 1;
        }
        final SocketChannel open = SocketChannel.open(new InetSocketAddress(hostname, port));
        Jvm.startup().on(getClass(), "Connected to " + hostname + ":" + port);
        return open;
    }

    /**
     * Adds a closeable resource to the registry's collection for tracking and proper cleanup.
     *
     * @param closeable The closeable resource to be tracked.
     */
    private void addCloseable(Closeable closeable) {
        closeableSet.add(closeable);
    }

    @Override
    protected synchronized void performClose() throws IllegalStateException {
        net.openhft.chronicle.core.io.Closeable.closeQuietly(closeableSet);
        closeableSet.clear();
    }
}
