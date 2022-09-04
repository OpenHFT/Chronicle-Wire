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
import net.openhft.chronicle.wire.channel.impl.BufferedChronicleChannel;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PerfThroughputMain {
    static final String URL = System.getProperty("url", "tcp://:1248");
    static final int RUN_TIME = Integer.getInteger("runTime", 10);
    static final int BATCH = Integer.getInteger("batch", 1);
    static final int CLIENTS = Integer.getInteger("clients", 4);
    static final boolean METHODS = Jvm.getBoolean("methods");

    public static void main(String[] args) {
        System.out.println("-Durl=" + URL + " " +
                "-DrunTime=" + RUN_TIME + " " +
                "-Dclients=" + CLIENTS + " " +
                "-Dbatch=" + BATCH + " " +
                "-Dmethods=" + METHODS
        );
        System.out.println("This is the total of the messages sent and messages received");
        int[] nClients = {CLIENTS};
        if (CLIENTS == 0)
            nClients = new int[]{16, 8, 4, 2, 1};
        for (int nClient : nClients) {
            try (ChronicleContext context = ChronicleContext.newContext(URL)) {
                EchoNHandler echoHandler = new EchoNHandler();
                echoHandler.times(BATCH);
                final ChronicleChannelSupplier supplier = context.newChannelSupplier(echoHandler);

                echoHandler.buffered(false);
                doTest("unbuffered", supplier.buffered(false), nClient);

                echoHandler.buffered(true);
                doTest("buffered", supplier.buffered(true), nClient);
            }
        }
    }

    private static void doTest(String desc, ChronicleChannelSupplier channelSupplier, int nClients) {
        InternalChronicleChannel[] clients = new InternalChronicleChannel[nClients];
        for (int i = 0; i < nClients; i++)
            clients[i] = (InternalChronicleChannel) channelSupplier.get();
        int bufferSize = clients[0].bufferSize();
        if (bufferSize < 4 << 20 && OS.isLinux()) {
            System.err.println("Try increasing the maximum buffer sizes");
            System.err.println("sudo sysctl --write net.core.rmem_max=2097152");
            System.err.println("sudo sysctl --write net.core.wmem_max=2097152");
        }
        for (int s = 1 << 20; s >= 8; s /= 2) {
            int size = Math.min(64, s);
            long start = System.currentTimeMillis();
            long end = start + RUN_TIME * 1000L;
            int window =  bufferSize /  (4 + size);
            if (size < 1024)
                window *= 2;
            if (nClients > 4)
                window = window * 4 / nClients;
            int finalWindow = window;
            AtomicLong totalRead = new AtomicLong(0);
            int finalSize = size;
            final Consumer<InternalChronicleChannel> sendAndReceive;

            if (METHODS) {
                if (size < 256) {
                    // send messages via MethodWriters
                    DummyDataSmall dd = new DummyDataSmall();
                    dd.data(new byte[size - Long.BYTES]);
                    sendAndReceive = icc -> {
                        int written = 0, read = 0;
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
                        int written = 0, read = 0;
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
                    int written = 0, read = 0;

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
            Stream.of(clients)
                    .parallel()
                    .forEach(sendAndReceive);

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

    private static int readUpto(int window, InternalChronicleChannel icc, int written, int read) {
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
