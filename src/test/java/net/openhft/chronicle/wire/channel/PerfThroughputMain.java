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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.channel.echo.DummyData;
import net.openhft.chronicle.wire.channel.echo.DummyDataSmall;
import net.openhft.chronicle.wire.channel.echo.EchoNHandler;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Main class for testing the throughput performance of the Chronicle system.
 */
public class PerfThroughputMain {

    // Configuration options
    static final String URL = System.getProperty("url", "tcp://:1248");
    static final int RUN_TIME = Integer.getInteger("runTime", 10);
    static final int BATCH = Integer.getInteger("batch", 1);
    static final int CLIENTS = Integer.getInteger("clients", 8);
    static final boolean METHODS = Jvm.getBoolean("methods");

    /**
     * The entry point of the application. This method sets up the testing environment and runs the throughput tests.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        // Output the configuration being used for the test
        System.out.println("-Durl=" + URL + " " +
                "-DrunTime=" + RUN_TIME + " " +
                "-Dclients=" + CLIENTS + " " +
                "-Dbatch=" + BATCH + " " +
                "-Dmethods=" + METHODS
        );
        System.out.println("This is the total of the messages sent and messages received");

        // Determine number of clients to test with
        int[] nClients = {CLIENTS};
        if (CLIENTS == 0)
            nClients = new int[]{16, 8, 4, 2, 1};

        // Loop through each client configuration and run the tests
        for (int nClient : nClients) {
            // Open a new Chronicle context for the test
            try (ChronicleContext context = ChronicleContext.newContext(URL)) {
                // Initialize the Echo handler with the specified batch size
                EchoNHandler echoHandler = new EchoNHandler();
                echoHandler.times(BATCH);

                // Create a new channel supplier for the test
                final ChronicleChannelSupplier supplier = context.newChannelSupplier(echoHandler);

                // Run unbuffered test
                echoHandler.buffered(false);
                doTest("unbuffered", supplier.buffered(false), nClient);

                // Run buffered test
                echoHandler.buffered(true);
                doTest("buffered", supplier.buffered(true), nClient);
            }
        }
    }

    /**
     * Perform a throughput test under specified conditions.
     *
     * @param desc             Description or name of the test, typically to signify the type of buffering.
     * @param channelSupplier  The supplier for the chronicle channels.
     * @param nClients         Number of client channels to create and test.
     */
    private static void doTest(String desc, ChronicleChannelSupplier channelSupplier, int nClients) {
        // Create and initialize client channels
        InternalChronicleChannel[] clients = new InternalChronicleChannel[nClients];
        for (int i = 0; i < nClients; i++)
            clients[i] = (InternalChronicleChannel) channelSupplier.get();

        int bufferSize = clients[0].bufferSize();

        // Check and warn if buffer size is insufficient on Linux
        if (bufferSize < 4 << 20 && OS.isLinux()) {
            System.err.println("Try increasing the maximum buffer sizes");
            System.err.println("sudo sysctl --write net.core.rmem_max=2097152");
            System.err.println("sudo sysctl --write net.core.wmem_max=2097152");
        }

        // Loop through each size and perform a throughput test
        for (int size = 1 << 20; size >= 8; size /= 2) {
            // Initialize test timing
            long start = System.currentTimeMillis();
            long end = start + RUN_TIME * 1000L;

            // Calculate window size based on buffer and number of clients
            int window = bufferSize /  (4 + size);
            if (size < 1024)
                window *= 2;
            if (nClients > 4)
                window = window * 4 / nClients;

            // Initialize atomic variables and test-specific functionality
            int finalWindow = window;
            AtomicLong totalRead = new AtomicLong(0);
            int finalSize = size;
            final Consumer<InternalChronicleChannel> sendAndReceive;

            // Configure the message sending mechanism depending on METHODS flag and message size
            if (METHODS) {
                if (size < 256) {
                    // send messages via MethodWriters
                    DummyDataSmall dd = new DummyDataSmall();
                    dd.data(new byte[size - Long.BYTES]);
                    sendAndReceive = icc -> {
                        long written = 0, read = 0;
                        final EchoingSmall echoing = icc.methodWriter(EchoingSmall.class);
                        do {
                            echoing.echo(dd);

                            // due to the multiplier in the EchoNHandler
                            written += BATCH;

                            read = readUpto(finalWindow, icc, written, read);
                        } while (System.currentTimeMillis() < end);

                        read = readUpto(0, icc, written, read);
                        totalRead.addAndGet(read);
                    };
                } else {
                    // send messages via MethodWriters
                    DummyData dd = new DummyData();
                    dd.data(new byte[size - Long.BYTES]);
                    sendAndReceive = icc -> {
                        long written = 0, read = 0;
                        final Echoing echoing = icc.methodWriter(Echoing.class);
                        do {
                            echoing.echo(dd);

                            // due to the multiplier in the EchoNHandler
                            written += BATCH;

                            read = readUpto(finalWindow, icc, written, read);
                        } while (System.currentTimeMillis() < end);

                        read = readUpto(0, icc, written, read);
                        totalRead.addAndGet(read);
                    };
                }
            } else {
                // send messages as raw bytes
                sendAndReceive = icc -> {
                    long written = 0, read = 0;

                    do {
                        final Bytes<?> bytes = icc.acquireProducer().bytes();
                        bytes.writeInt(finalSize);
                        bytes.writeSkip(finalSize);
                        icc.releaseProducer();

                        // due to the multiplier in the EchoNHandler
                        written += BATCH;

                        read = readUpto(finalWindow, icc, written, read);
                    } while (System.currentTimeMillis() < end);

                    read = readUpto(0, icc, written, read);
                    totalRead.addAndGet(read);
                };
            }

            // Execute the configured test on all clients in parallel
            Stream.of(clients)
                    .parallel()
                    .forEach(sendAndReceive);

            // Calculate and print the results
            long count = totalRead.get();
            long time = System.currentTimeMillis() - start;
            long totalBytes = size * count;
            double GBps = (totalBytes + totalBytes / BATCH) / (time / 1e3) / 1e9;
            long rate = (count + count / BATCH) * 1000 / time;
//            if (s != size)
//                System.out.println("Warmup...");
//            else
            System.out.printf("clients; %d; desc; %s; size; %,6d; GB/s; %6.3f; Mmsg/s; %6.3f%n",
                    nClients, desc, size, GBps, rate/1e6);
        }
    }

    /**
     * Reads messages from the channel up to the specified window size.
     *
     * @param window   The number of messages to read.
     * @param icc      The channel from which messages are to be read.
     * @param written  The number of messages written to the channel.
     * @param read     The number of messages already read from the channel.
     * @return         The updated number of messages read from the channel.
     */
    private static long readUpto(int window, InternalChronicleChannel icc, long written, long read) {
        // Continuously read messages until the unread window is below the specified size
        do {
            try (DocumentContext dc = icc.readingDocument()) {
                if (dc.isPresent())
                    read++;
            }
            Jvm.nanoPause();
        } while (written - read > window);
        return read;
    }
}
