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
 * The {@code ChronicleGatewayMain} class represents the primary access point for the Chronicle Gateway.
 * This gateway is pivotal in processing incoming connections and effectively handling requests based on a predetermined protocol.
 * <p>
 * By extending {@link ChronicleContext} and implementing {@link Closeable} and {@link Runnable}, instances of this class
 * have the ability to manage their lifecycle autonomously and can operate in individual threads. Configuration of the Gateway
 * can be done utilizing system properties.
 *
 * @since 2023-09-15
 */
public class ChronicleGatewayMain extends ChronicleContext implements Closeable, Runnable {

    // Default port for the gateway
    public static final int PORT = Integer.getInteger("port", 1248);

    // Determines the pausing strategy for the gateway
    private static final PauserMode PAUSER_MODE = PauserMode.valueOf(
            System.getProperty("pauserMode", PauserMode.balanced.name()));

    // Flag to determine if affinity should be used
    private static final boolean USE_AFFINITY = Jvm.getBoolean("useAffinity");

    // Server socket channel to handle network communications
    transient ServerSocketChannel ssc;

    // Thread for executing the gateway
    transient Thread thread;

    // Determines the pausing strategy for buffered channels
    @Comment("PauserMode to use in buffered channels")
    PauserMode pauserMode = PauserMode.balanced;

    // Buffering state for the handler
    @Comment("Default buffering configuration if not set by the Handler")
    private boolean buffered = false;

    // Service to manage thread execution
    private ExecutorService service;

    /**
     * Creates a ChronicleGatewayMain instance with a specified URL. The gateway is initialized with
     * the default system context and a fresh socket registry.
     *
     * @param url The URL for the Gateway's operations.
     * @throws InvalidMarshallableException if there are issues initializing the gateway.
     */
    public ChronicleGatewayMain(String url) throws InvalidMarshallableException {
        this(url, new SocketRegistry(), SystemContext.INSTANCE);
        addCloseable(socketRegistry());
    }

    /**
     * Constructs a ChronicleGatewayMain with specified parameters: URL, socket registry, and system context.
     * This allows more precise configuration for advanced use cases.
     *
     * @param url            The URL for the Gateway's operations.
     * @param socketRegistry Manages socket connections for the gateway.
     * @param systemContext  Sets the system-level configurations for the gateway.
     * @throws InvalidMarshallableException if issues arise during gateway initialization.
     */
    public ChronicleGatewayMain(String url, SocketRegistry socketRegistry, SystemContext systemContext) throws InvalidMarshallableException {
        super(url, socketRegistry);
        this.systemContext(systemContext);
    }

    /**
     * Serves as the initiation point for the {@code ChronicleGatewayMain}. A new class instance is created
     * and set into action. If a command-line argument provides the URL for the gateway, it's used.
     * Otherwise, a default is taken.
     *
     * @param args Command line arguments. The first argument, if given, sets the gateway's URL.
     * @throws IOException                  Potential I/O problems during gateway startup.
     * @throws InvalidMarshallableException Issues that might arise during gateway creation.
     */
    public static void main(String... args) throws IOException, InvalidMarshallableException {
        main(ChronicleGatewayMain.class, ChronicleGatewayMain::new, args.length == 0 ? "" : args[0]).run();
    }

    /**
     * Generates an instance of the {@code ChronicleGatewayMain} class based on the supplied configuration.
     *
     * @param <T>      Represents the subclass type of {@code ChronicleGatewayMain}.
     * @param mainClass The main class type for creating an instance.
     * @param supplier  Provides an instance of the main class based on the configuration string.
     * @param config    The configuration string used for creating the main instance.
     * @return An instance of the {@code ChronicleGatewayMain} class, created and configured based on the provided arguments.
     * @throws IOException If there's a problem reading the configuration.
     */
    protected static <T extends ChronicleGatewayMain> ChronicleGatewayMain main(Class<T> mainClass, Function<String, T> supplier, String config) throws IOException {
        ChronicleGatewayMain main;
        if (config.isEmpty()) {
            // Default configuration setup for the gateway
            ChronicleGatewayMain chronicleGatewayMain =
                    supplier.apply("tcp://localhost:" + PORT)
                            .pauserMode(PAUSER_MODE)
                            .buffered(Jvm.getBoolean("buffered"));
            chronicleGatewayMain.useAffinity(USE_AFFINITY);
            chronicleGatewayMain.pauserMode = PAUSER_MODE;
            main = chronicleGatewayMain;
        } else {
            // Load the gateway configuration from the specified file
            main = Marshallable.fromFile(mainClass, config);
        }
        return main;
    }

    /**
     * Configures the {@code PauserMode} for the gateway to find the right balance between CPU consumption and latency minimization.
     *
     * @param pauserMode The pauser mode to be set.
     * @return The current instance of {@code ChronicleGatewayMain} for chained calls.
     */
    public ChronicleGatewayMain pauserMode(PauserMode pauserMode) {
        this.pauserMode = pauserMode;
        return this;
    }

    /**
     * Checks if the gateway is operating in buffered mode.
     *
     * @return {@code true} if in buffered mode, otherwise {@code false}.
     */
    public boolean buffered() {
        return buffered;
    }

    /**
     * Specifies whether new connections will use buffering. If a client doesn't indicate the buffering preference,
     * this setting determines the buffering state.
     *
     * @param buffered Set to {@code true} if buffering should be enabled.
     * @return The current instance of {@code ChronicleGatewayMain} for chained calls.
     */
    public ChronicleGatewayMain buffered(boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    /**
     * Activates the gateway. This involves establishing the server socket channel and, if not already operational,
     * firing up the acceptor thread.
     *
     * @throws IOException If there's a hiccup during the gateway's startup.
     */
    public synchronized ChronicleGatewayMain start() throws IOException {
        if (isClosed())
            throw new IllegalStateException("Closed");
        bindSSC();
        if (thread == null) {
            // Initializing the acceptor thread
            thread = new Thread(this::run, "acceptor");
            thread.setDaemon(true);
            thread.start();
        }
        return this;
    }

    /**
     * Secures the server socket channel if it hasn't been initialized yet.
     *
     * @throws IOException If there's an issue accessing or creating the socket channel.
     */
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

    /**
     * Waits for the service to terminate, giving it 1 second before proceeding.
     * If interrupted, it logs a warning and re-interrupts the current thread.
     */
    private void waitForService() {
        try {
            service.shutdownNow();
            service.awaitTermination(1, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            // Logging the interruption and setting the interrupt flag back
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
     * Manages an incoming connection. This involves decoding the request and dispatching it
     * to the correct handler based on the read channel header. The method ensures that proper
     * error handling is in place for various exceptions, including HTTP GET detection and
     * invalid protocol scenarios.
     *
     * @param channel The incoming TCP channel to be managed.
     */
    void handle(TCPChronicleChannel channel) {
        // Default to closing the channel when done handling
        boolean close = true;
        ChronicleChannel channel2 = null;
        try {
            // Fetching the inbound channel header
            final ChannelHeader channelHeader = channel.headerInToUse();

            // Validate the channel header to determine the correct handler
            ChannelHandler bh = validateHandler(channelHeader);
            if (bh == null) return;

            // Check if buffering is enabled by default or as specified by the handler
            boolean buffered = this.buffered;
            if (bh.buffered() != null)
                buffered = bh.buffered();
            Jvm.debug().on(ChronicleGatewayMain.class, "Server got " + bh);

            // Obtain the outbound channel header
            final ChannelHeader headerOut = channel.headerOut();

            // Handling the scenario where the outbound header indicates a redirect
            if (headerOut instanceof RedirectHeader) {
                System.out.println("Server redirected  " + headerOut);
                return;
            }

            // Depending on buffering, either wrap the channel or use it as is
            channel2 = buffered
                    ? new BufferedChronicleChannel(channel, pauserMode.get())
                    : channel;

            Jvm.debug().on(ChronicleGatewayMain.class, "Running " + channel2);

            // Execute the determined handler on the channel
            bh.run(this, channel2);

            // Decide if the channel should be closed after handling
            close = bh.closeWhenRunEnds();

        } catch (HTTPDetectedException e) {
            // Special logging for when an HTTP GET request is encountered
            Jvm.warn().on(getClass(), "HTTP GET Detected", e);

        } catch (InvalidProtocolException e) {
            // Special logging for when an invalid protocol is used
            Jvm.warn().on(getClass(), "Invalid Protocol", e);

        } catch (Throwable t) {
            // A short pause before checking the closing status of the resources
            Jvm.pause(1);

            // If neither the main object nor the channel are closing, log the exception
            if (!isClosing() && !channel.isClosing())
                if (t instanceof ClosedIORuntimeException)
                    Jvm.warn().on(getClass(), t.toString());
                else
                    Jvm.error().on(getClass(), t);
        } finally {
            // Clean up channels if they need to be closed
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
