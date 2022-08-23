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
public class SocketRegistry extends AbstractCloseable {
    private final Map<String, ServerSocketChannel> descToServerSocketChannelMap = new ConcurrentSkipListMap<>();
    private final Set<Closeable> closeableSet =
            Collections.synchronizedSet(
                    Collections.newSetFromMap(
                            new WeakIdentityHashMap<>()));
    // not thread safe but it doesn't matter
    int lastHost = 0;

    public ServerSocketChannel acquireServerSocketChannel(URL url) throws IOException {
        return acquireServerSocketChannel(url.getHost(), url.getPort());
    }

    private synchronized ServerSocketChannel acquireServerSocketChannel(String hostname, int port) throws IOException {
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

    public synchronized SocketChannel createSocketChannel(String hostname, int port) throws IOException {
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

    private void addCloseable(Closeable closeable) {
        closeableSet.add(closeable);
    }

    @Override
    protected synchronized void performClose() throws IllegalStateException {
        net.openhft.chronicle.core.io.Closeable.closeQuietly(closeableSet);
        closeableSet.clear();
    }
}
