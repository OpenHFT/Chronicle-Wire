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

/**
 * Represents the entry point for a Chronicle Gateway, which is responsible for accepting
 * incoming connections and handling requests according to a defined protocol.
 * <p>
 * This class extends {@link ChronicleContext} and implements {@link Closeable} and {@link Runnable}.
 * Therefore, instances of this class can manage their own lifecycle and can be executed in separate threads.
 * The Gateway can be configured using system properties.
 */
public class ChronicleGatewayMain extends ChronicleContext implements Closeable, Runnable {
    public static final int PORT = Integer.getInteger("port", 1248);
    private static final PauserMode PAUSER_MODE = PauserMode.valueOf(
            System.getProperty("pauserMode", PauserMode.balanced.name()));
    private static final boolean USE_AFFINITY = Jvm.getBoolean("useAffinity");
    transient ServerSocketChannel ssc;
    transient Thread thread;
    @Comment("PauserMode to use in buffered channels")
    PauserMode pauserMode = PauserMode.balanced;
    @Comment("Default buffering configuration if not set by the Handler")
    private boolean buffered = false;
    private ExecutorService service;

    /**
     * Constructs a new ChronicleGatewayMain instance with a specific URL.
     * The gateway will use the default system context and a new socket registry.
     *
     * @param url the URL for the Gateway
     * @throws InvalidMarshallableException if there's an issue while creating the gateway
     */
    public ChronicleGatewayMain(String url) throws InvalidMarshallableException {
        this(url, new SocketRegistry(), SystemContext.INSTANCE);
        addCloseable(socketRegistry());
    }

    /**
     * Constructs a new ChronicleGatewayMain instance with a specific URL,
     * socket registry, and system context.
     *
     * @param url            the URL for the Gateway
     * @param socketRegistry the SocketRegistry used by the gateway for managing socket connections
     * @param systemContext  the SystemContext that defines system-level configuration for the gateway
     * @throws InvalidMarshallableException if there's an issue while creating the gateway
     */
    public ChronicleGatewayMain(String url, SocketRegistry socketRegistry, SystemContext systemContext) throws InvalidMarshallableException {
        super(url, socketRegistry);
        this.systemContext(systemContext);
    }

    /**
     * The main method acts as the entry point to start the ChronicleGatewayMain.
     * It creates a new instance of the class and runs it.
     * The URL for the gateway can be optionally specified as a command-line argument.
     *
     * @param args Command line arguments. If the first argument is provided, it will be used as the URL for the gateway.
     * @throws IOException                  if there is an I/O issue while starting the gateway
     * @throws InvalidMarshallableException if there's an issue while creating the gateway
     */
    public static void main(String... args) throws IOException, InvalidMarshallableException {
        main(ChronicleGatewayMain.class, ChronicleGatewayMain::new, args.length == 0 ? "" : args[0]).run();
    }

    protected static <T extends ChronicleGatewayMain> ChronicleGatewayMain main(Class<T> mainClass, Function<String, T> supplier, String config) throws IOException {
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
        return main;
    }

    /**
     * Sets the PauserMode to control the balance between CPU usage and minimising latency
     *
     * @param pauserMode to use
     * @return this
     */
    public ChronicleGatewayMain pauserMode(PauserMode pauserMode) {
        this.pauserMode = pauserMode;
        return this;
    }

    /**
     * @return is it running in buffered mode
     */
    public boolean buffered() {
        return buffered;
    }

    /**
     * Sets whether new connections will be buffered if the client doesn't specify whether buffering should be used.
     *
     * @param buffered if true.
     * @return this
     */
    public ChronicleGatewayMain buffered(boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    /**
     * Starts the gateway, binding the server socket channel and starting the acceptor thread if not already running.
     *
     * @throws IOException if there is a problem starting the gateway
     */
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

    /**
     * Main execution loop for the gateway. Accepts incoming connections and handles requests.
     */
    @Override
    public void run() {
        // Jvm.startup().on(getClass(), "Starting  " + this);
        service = Executors.newCachedThreadPool(new NamedThreadFactory("connections"));
        Throwable thrown = null;
        try {
            bindSSC();
            ChronicleChannelCfg channelCfg = new ChronicleChannelCfg().port(url().getPort()).pauserMode(pauserMode).buffered(buffered);
            while (!isClosed()) {
                final SocketChannel sc = ssc.accept();
                sc.socket().setTcpNoDelay(true);
                final TCPChronicleChannel channel = new TCPChronicleChannel(systemContext(), channelCfg, sc, this::replaceInHeader, this::replaceOutHeader);
                channel.closeCallback(closeCallback());
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

    /**
     * Allows replacing of the inbound channel header.
     * By default, it retains the given header without making any changes.
     *
     * @param channelHeader the inbound channel header
     * @return the possibly replaced channel header
     */
    protected ChannelHeader replaceInHeader(ChannelHeader channelHeader) {
        return channelHeader;
    }

    /**
     * Allows replacing of the outbound channel header.
     * If the outbound channel header is an instance of a ChannelHandler,
     * it replaces it with the response header from the handler.
     *
     * @param channelHeader the outbound channel header
     * @return the possibly replaced channel header
     */
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

    /**
     * Handles an incoming connection, decoding the request and dispatching to the appropriate handler.
     *
     * @param channel the channel representing the incoming connection
     */
    void handle(TCPChronicleChannel channel) {
        // Indicate whether the channel should be closed when done handling
        boolean close = true;
        ChronicleChannel channel2 = null;
        try {
            // Retrieve the inbound channel header
            final ChannelHeader channelHeader = channel.headerInToUse();

            // Validate the channel header and retrieve the handler
            ChannelHandler bh = validateHandler(channelHeader);
            if (bh == null) return;

            // Determine whether buffering is enabled
            boolean buffered = this.buffered;
            if (bh.buffered() != null)
                buffered = bh.buffered();
            Jvm.debug().on(ChronicleGatewayMain.class, "Server got " + bh);

            // Retrieve the outbound channel header
            final ChannelHeader headerOut = channel.headerOut();

            // If the outbound channel header is a redirect, print a message and return
            if (headerOut instanceof RedirectHeader) {
                System.out.println("Server redirected  " + headerOut);
                return;
            }

            // Instantiate the secondary channel based on whether buffering is enabled
            channel2 = buffered
                    ? new BufferedChronicleChannel(channel, pauserMode.get())
                    : channel;

            Jvm.debug().on(ChronicleGatewayMain.class, "Running " + channel2);

            // Run the channel handler
            bh.run(this, channel2);

            // Determine whether to close the channel when done
            close = bh.closeWhenRunEnds();

        } catch (HTTPDetectedException e) {
            // Handle the exception case where an HTTP GET request is detected
            Jvm.warn().on(getClass(), "HTTP GET Detected", e);

        } catch (InvalidProtocolException e) {
            // Handle the exception case where an invalid protocol is detected
            Jvm.warn().on(getClass(), "Invalid Protocol", e);

        } catch (Throwable t) {
            // Pause for a moment before checking if the resource is closing
            Jvm.pause(1);

            // Log any other exceptions if not closing
            if (!isClosing() && !channel.isClosing())
                if (t instanceof ClosedIORuntimeException)
                    Jvm.warn().on(getClass(), t.toString());
                else
                    Jvm.error().on(getClass(), t);
        } finally {
            // Close the channels if the close flag is set
            if (close)
                Closeable.closeQuietly(channel2, channel);
        }
    }

    /**
     * Validates that the incoming request contains a valid handler.
     *
     * @param marshallable the incoming request
     * @return a ChannelHandler instance if the request is valid; otherwise null
     */
    @Nullable
    protected ChannelHandler validateHandler(Marshallable marshallable) {
        if (!(marshallable instanceof ChannelHandler)) {
            return new ErrorReplyHandler().errorMsg("The header must be a ChannelHandler");
        }
        return (ChannelHandler) marshallable;
    }

    /**
     * Returns the port number on which the gateway is listening.
     *
     * @return the port number
     */
    public int port() {
        return ssc.socket().getLocalPort();
    }
}
