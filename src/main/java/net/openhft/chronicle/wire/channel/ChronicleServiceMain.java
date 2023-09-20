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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.threads.NamedThreadFactory;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wires;
import net.openhft.chronicle.wire.channel.impl.BufferedChronicleChannel;
import net.openhft.chronicle.wire.channel.impl.TCPChronicleChannel;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Represents the main class for the Chronicle Service which is responsible for
 * accepting and managing incoming connections using multiple threads.
 * <p>
 * This class is a type of {@link SelfDescribingMarshallable} and implements the {@link Closeable}
 * interface. As a result, instances of this class can manage their own lifecycle.
 * It allows configuration through marshalling and can be controlled via system properties.
 *
 * @since 2023-09-15
 */
public class ChronicleServiceMain extends SelfDescribingMarshallable implements Closeable {
    int port;  // The port on which the server listens for incoming connections
    Marshallable microservice;  // The microservice configuration or definition (not used in the provided code)
    boolean buffered;  // Flag to determine whether the service should use buffered channels
    transient ServerSocketChannel ssc;  // Server socket channel for accepting incoming connections
    transient volatile boolean closed;  // Flag to track whether the service is closed
    transient Set<ChronicleChannel> channels;  // A set of active channels managed by this service

    /**
     * The main method acts as the entry point to start the ChronicleServiceMain.
     * It creates a new instance of the class from a specified configuration file and runs it.
     *
     * @param args Command line arguments. The first argument is expected to be the path to the configuration file.
     * @throws IOException                  if there's an I/O issue
     * @throws InvalidMarshallableException if there's an issue with marshalling
     */
    public static void main(String... args) throws IOException, InvalidMarshallableException {
        ChronicleServiceMain main = Marshallable.fromFile(ChronicleServiceMain.class, args[0]);
        main.buffered = Jvm.getBoolean("buffered", main.buffered);
        main.run();
    }

    /**
     * Starts the service by opening a server socket channel on the specified port and
     * managing incoming connections using multiple threads.
     * Each incoming connection is handled by a separate thread using a `ConnectionHandler`.
     */
    void run() {
        channels = Collections.newSetFromMap(new WeakHashMap<>());  // Initialize the set of channels using a weak hash map

        Jvm.startup().on(getClass(), "Starting " + this);
        Thread.currentThread().setName("acceptor");
        ExecutorService service = Executors.newCachedThreadPool(new NamedThreadFactory("connections"));  // Thread pool to manage incoming connections
        try {
            ssc = ServerSocketChannel.open();  // Open the server socket channel
            ssc.bind(new InetSocketAddress(port));  // Bind it to the specified port
            ChronicleChannelCfg channelCfg = new ChronicleChannelCfg().port(port);  // Configuration for the channel with the specified port
            Function<ChannelHeader, ChannelHeader> redirectFunction = this::replaceOutHeader;  // Function to redirect channel headers (method not provided in the code)

            // Continuously accept incoming connections until the service is closed
            while (!isClosed()) {
                final SocketChannel sc = ssc.accept();
                sc.socket().setTcpNoDelay(true);
                final TCPChronicleChannel connection0 = new TCPChronicleChannel(SystemContext.INSTANCE, channelCfg, sc, h -> h, redirectFunction);
                ChronicleChannel channel = buffered ? new BufferedChronicleChannel(connection0, Pauser.balanced()) : connection0;
                channels.add(channel);
                service.submit(() -> new ConnectionHandler(channel).run());
            }
        } catch (Throwable e) {
            if (!isClosed()) Jvm.error().on(getClass(), e);  // Log any exceptions if the service isn't closed
        } finally {
            close();
            Jvm.pause(100);
            // Synchronize on Wires class to avoid concurrent modifications while shutting down
            synchronized (Wires.class) {
                AffinityLock.dumpLocks();  // Dump affinity locks for debugging
                service.shutdownNow();  // Shutdown the thread pool
            }
            try {
                service.awaitTermination(1, TimeUnit.SECONDS);  // Wait for termination of the thread pool
            } catch (InterruptedException e) {
                Jvm.warn().on(getClass(), e);
                Thread.currentThread().interrupt();  // Set the interrupt flag if interrupted
            }
        }
    }

    /**
     * Replaces the outbound header for the channel based on its type.
     *
     * @param channelHandler the current channel header
     * @return a new channel header, either an OkHeader or a RedirectHeader
     */
    protected ChannelHeader replaceOutHeader(ChannelHeader channelHandler) {
        if (channelHandler instanceof OkHeader)
            return new OkHeader();
        //noinspection unchecked
        return new RedirectHeader(Collections.EMPTY_LIST);
    }

    @Override
    public void close() {
        closed = true;
        Closeable.closeQuietly(ssc);
        Closeable.closeQuietly(channels);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Inner class to represent the connection handler.
     * This class is responsible for handling individual incoming connections and processing
     * their associated messages/events using the service's microservice logic.
     */
    class ConnectionHandler {
        final ChronicleChannel channel;  // The channel associated with this connection

        /**
         * Constructor to initialize the handler with a specific channel.
         *
         * @param channel the channel to be managed by this handler
         */
        public ConnectionHandler(ChronicleChannel channel) {
            this.channel = channel;
        }

        /**
         * Runs the connection handler logic.
         * This method manages the lifecycle of the channel's connection, processes messages/events
         * using the service's microservice logic, and handles various exceptions.
         */
        void run() {
            try {
                Jvm.debug().on(ChronicleServiceMain.class, "Server got " + channel.headerIn());

                // Deep copy of the main microservice instance
                final Marshallable microservice = ChronicleServiceMain.this.microservice.deepCopy();

                // Reflection to get the 'out' field from the microservice class
                final Field field = Jvm.getFieldOrNull(microservice.getClass(), "out");
                if (field == null)
                    throw new IllegalStateException("Microservice " + microservice + " must have a field called out");

                // Obtain a method writer proxy of the appropriate type for this channel
                Object out = channel.methodWriter(field.getType());

                // The AffinityLock is likely used here to bind the executing thread to a CPU core
                try (AffinityLock lock = AffinityLock.acquireLock()) {
                    field.set(microservice, out);

                    // Run the microservice's event handler logic
                    channel.eventHandlerAsRunnable(microservice).run();

                } catch (ClosedIORuntimeException e) {
                    Thread.yield();
                    if (!((Closeable) microservice).isClosed())
                        Jvm.debug().on(getClass(), "readOne threw " + e);

                } catch (Exception e) {
                    Thread.yield();
                    if (!((Closeable) microservice).isClosed() && !channel.isClosed())
                        Jvm.warn().on(getClass(), "readOne threw ", e);
                }
            } catch (Throwable t) {
                Jvm.error().on(getClass(), t);

            } finally {
                Closeable.closeQuietly(channel);  // Close the channel when done
            }
        }
    }
}
