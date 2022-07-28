package net.openhft.chronicle.wire.channel.impl;

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
        return SocketChannel.open(new InetSocketAddress(hostname, port));
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
