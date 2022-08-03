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
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.threads.NamedThreadFactory;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.Comment;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.channel.impl.BufferedChronicleChannel;
import net.openhft.chronicle.wire.channel.impl.SocketRegistry;
import net.openhft.chronicle.wire.channel.impl.TCPChronicleChannel;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class ChronicleGatewayMain extends ChronicleContext implements Closeable {
    private final transient Function<ChannelHeader, ChannelHeader> redirectFunction;
    transient ServerSocketChannel ssc;
    transient Thread thread;
    @Comment("PauserMode to use in buffered channels")
    PauserMode pauserMode = PauserMode.balanced;
    @Comment("Default buffered if not set by the Handler")
    private boolean buffered = false;
    private ExecutorService service;

    public ChronicleGatewayMain(String url) {
        this(url, new SocketRegistry(), SystemContext.INSTANCE);
        addCloseable(socketRegistry());
    }

    public ChronicleGatewayMain(String url, SocketRegistry socketRegistry, SystemContext systemContext) {
        super(url, socketRegistry);
        this.systemContext(systemContext);
        redirectFunction = this::redirect;
    }

    public static void main(String... args) throws IOException {
        ChronicleGatewayMain chronicleGatewayMain = new ChronicleGatewayMain("tcp://localhost:" + Integer.getInteger("port", 1248))
                .pauserMode(PauserMode.valueOf(System.getProperty("pauserMode", PauserMode.balanced.name())))
                .buffered(Jvm.getBoolean("buffered"));
        chronicleGatewayMain.useAffinity(Jvm.getBoolean("useAffinity"));
        chronicleGatewayMain.pauserMode = PauserMode.valueOf(System.getProperty("pauserMode", PauserMode.balanced.name()));
        ChronicleGatewayMain main = args.length == 0
                ? chronicleGatewayMain
                : Marshallable.fromFile(ChronicleGatewayMain.class, args[0]);
        System.out.println("Starting  " + main);
        main.run();
    }

    public ChronicleGatewayMain pauserMode(PauserMode pauserMode) {
        this.pauserMode = pauserMode;
        return this;
    }

    public boolean buffered() {
        return buffered;
    }

    public ChronicleGatewayMain buffered(boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    public synchronized ChronicleGatewayMain start() throws IOException {
        if (isClosed())
            throw new IllegalStateException("Closed");
        bindSSC();
        if (thread == null) {
            thread = new Thread(this::run, "acceptor");
            thread.setDaemon(true);
            thread.start();
        }
        return this;
    }

    private void bindSSC() throws IOException {
        if (ssc == null) {
            ssc = socketRegistry().acquireServerSocketChannel(url());
        }
    }

    private void run() {
        service = Executors.newCachedThreadPool(new NamedThreadFactory("connections"));
        Throwable thrown = null;
        try {
            bindSSC();
            ChronicleChannelCfg channelCfg = new ChronicleChannelCfg().port(url().getPort()).pauserMode(pauserMode).buffered(buffered);
            while (!isClosed()) {
                final SocketChannel sc = ssc.accept();
                sc.socket().setTcpNoDelay(true);
                final TCPChronicleChannel channel = new TCPChronicleChannel(this, channelCfg, sc, redirectFunction);
                service.submit(() -> handle(channel));
            }
        } catch (Throwable e) {
            thrown = e;

        } finally {
            Thread.yield();
            boolean closing = isClosing() || socketRegistry().isClosing();
            close();
            if (thrown != null && !closing)
                Jvm.error().on(getClass(), thrown);
        }
    }

    private void waitForService() {
        try {
            service.shutdownNow();
            service.awaitTermination(1, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Jvm.warn().on(getClass(), e);
            Thread.currentThread().interrupt();
        }
    }

    protected ChannelHeader redirect(ChannelHeader channelHandler) {
        return null;
    }

    @Override
    protected void performClose() {
        super.performClose();
        Closeable.closeQuietly(ssc);

        if (service != null)
            waitForService();
    }

    void handle(TCPChronicleChannel channel) {
        boolean close = true;
        ChronicleChannel channel2 = null;
        try {
            // get the header
            final Marshallable marshallable = channel.headerIn(redirectFunction);
            if (!(marshallable instanceof ChannelHandler)) {
                try (DocumentContext dc = channel.acquireWritingDocument(true)) {
                    dc.wire().write("error").text("The header must be a BrokerHandler");
                }
                return;
            }
            ChannelHandler bh = (ChannelHandler) marshallable;
            boolean buffered = this.buffered;
            if (bh.buffered() != null)
                buffered = bh.buffered();
            System.out.println("Server got " + bh);
            final ChannelHeader headerOut = channel.headerOut();
            if (headerOut instanceof RedirectHeader) {
                System.out.println("Server redirected  " + headerOut);
                return;
            }
            channel2 = buffered
                    ? new BufferedChronicleChannel(channel, pauserMode.get(), redirectFunction)
                    : channel;
            System.out.println("Running " + channel2);
            bh.run(this, channel2);
            close = bh.closeWhenRunEnds();
        } catch (Throwable t) {
            Jvm.pause(1);
            if (!isClosing() && !channel.isClosing())
                if (t instanceof ClosedIORuntimeException)
                    Jvm.warn().on(getClass(), t.toString());
                else
                    Jvm.error().on(getClass(), t);
        } finally {
            if (close)
                Closeable.closeQuietly(channel2, channel);
        }
    }

    public int port() {
        return ssc.socket().getLocalPort();
    }
}
