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
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.threads.NamedThreadFactory;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.Comment;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.channel.impl.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class ChronicleGatewayMain extends ChronicleContext implements Closeable {
    public static final int PORT = Integer.getInteger("port", 1248);
    private static final PauserMode PAUSER_MODE = PauserMode.valueOf(
            System.getProperty("pauserMode", PauserMode.balanced.name()));
    private static final boolean USE_AFFINITY = Jvm.getBoolean("useAffinity");
    transient ServerSocketChannel ssc;
    transient Thread thread;
    @Comment("PauserMode to use in buffered channels")
    PauserMode pauserMode = PauserMode.balanced;
    @Comment("Default buffered if not set by the Handler")
    private boolean buffered = false;
    private ExecutorService service;

    public ChronicleGatewayMain(String url) throws InvalidMarshallableException {
        this(url, new SocketRegistry(), SystemContext.INSTANCE);
        addCloseable(socketRegistry());
    }

    public ChronicleGatewayMain(String url, SocketRegistry socketRegistry, SystemContext systemContext) throws InvalidMarshallableException {
        super(url, socketRegistry);
        this.systemContext(systemContext);
    }

    public static void main(String... args) throws IOException, InvalidMarshallableException {
        main(ChronicleGatewayMain.class, ChronicleGatewayMain::new, args.length == 0 ? "" : args[0]);
    }

    protected static <T extends ChronicleGatewayMain> void main(Class<T> mainClass, Function<String, T> supplier, String config) throws IOException {
        ChronicleGatewayMain main;
        if (config.isEmpty()) {
            ChronicleGatewayMain chronicleGatewayMain =
                    supplier.apply("tcp://localhost:" + PORT)
                            .pauserMode(PAUSER_MODE)
                            .buffered(Jvm.getBoolean("buffered"));
            chronicleGatewayMain.useAffinity(USE_AFFINITY);
            chronicleGatewayMain.pauserMode = PAUSER_MODE;
            main = chronicleGatewayMain;
        } else {
            main = Marshallable.fromFile(mainClass, config);
        }
        Jvm.startup().on(mainClass, "Starting  " + main);
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
                final TCPChronicleChannel channel = new TCPChronicleChannel(systemContext(), channelCfg, sc, this::replaceInHeader, this::replaceOutHeader);
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

    protected ChannelHeader replaceInHeader(ChannelHeader channelHeader) {
        return channelHeader;
    }

    protected ChannelHeader replaceOutHeader(ChannelHeader channelHeader) {
        if (channelHeader instanceof ChannelHandler) {
            ChannelHandler handler = (ChannelHandler) channelHeader;
            return handler.responseHeader(this);
        }
        return channelHeader;
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
            final ChannelHeader channelHeader = channel.headerInToUse();
            ChannelHandler bh = validateHandler(channelHeader);
            if (bh == null) return;
            boolean buffered = this.buffered;
            if (bh.buffered() != null)
                buffered = bh.buffered();
            Jvm.debug().on(ChronicleGatewayMain.class, "Server got " + bh);
            final ChannelHeader headerOut = channel.headerOut();
            if (headerOut instanceof RedirectHeader) {
                System.out.println("Server redirected  " + headerOut);
                return;
            }
            channel2 = buffered
                    ? new BufferedChronicleChannel(channel, pauserMode.get())
                    : channel;
            Jvm.debug().on(ChronicleGatewayMain.class, "Running " + channel2);
            bh.run(this, channel2);
            close = bh.closeWhenRunEnds();

        } catch (HTTPDetectedException e) {
            Jvm.warn().on(getClass(), "HTTP GET Detected", e);

        } catch (InvalidProtocolException e) {
            Jvm.warn().on(getClass(), "Invalid Protocol", e);

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

    @Nullable
    protected ChannelHandler validateHandler(Marshallable marshallable) {
        if (!(marshallable instanceof ChannelHandler)) {
            return new ErrorReplyHandler().errorMsg("The header must be a ChannelHandler");
        }
        return (ChannelHandler) marshallable;
    }

    public int port() {
        return ssc.socket().getLocalPort();
    }
}
