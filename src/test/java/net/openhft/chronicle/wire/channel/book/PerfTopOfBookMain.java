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

package net.openhft.chronicle.wire.channel.book;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.channel.ChronicleChannelSupplier;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.InternalChronicleChannel;
import net.openhft.chronicle.wire.channel.echo.EchoHandler;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/*
On a Ryzen 5950X, bare metal Ubuntu 21.10
clients; 32; desc; buffered; size;     44; GB/s;  1.446; Mmsg/s; 32.854
clients; 32; desc; buffered; size;     44; GB/s;  1.654; Mmsg/s; 37.600
clients; 32; desc; buffered; size;     44; GB/s;  1.666; Mmsg/s; 37.859
clients; 16; desc; buffered; size;     44; GB/s;  1.777; Mmsg/s; 40.384
clients; 16; desc; buffered; size;     44; GB/s;  1.780; Mmsg/s; 40.451
clients; 16; desc; buffered; size;     44; GB/s;  1.772; Mmsg/s; 40.272
clients; 8; desc; buffered; size;     44; GB/s;  1.356; Mmsg/s; 30.822
clients; 8; desc; buffered; size;     44; GB/s;  1.362; Mmsg/s; 30.950
clients; 8; desc; buffered; size;     44; GB/s;  1.355; Mmsg/s; 30.791
clients; 4; desc; buffered; size;     44; GB/s;  0.880; Mmsg/s; 20.007
clients; 4; desc; buffered; size;     44; GB/s;  0.731; Mmsg/s; 16.619
clients; 4; desc; buffered; size;     44; GB/s;  0.737; Mmsg/s; 16.751
 */
public class PerfTopOfBookMain {
    static final String URL = System.getProperty("url", "tcp://:1248");
    static final int RUN_TIME = Integer.getInteger("runTime", 20);
    static final int CLIENTS = Integer.getInteger("clients", 0);

    public static void main(String[] args) {
        System.out.println("-Durl=" + URL + " " +
                "-DrunTime=" + RUN_TIME + " " +
                "-Dclients=" + CLIENTS
        );
        System.out.println("This is the total of the messages sent and messages received");
        int[] nClients = {CLIENTS};
        if (CLIENTS == 0)
            nClients = new int[]{32, 16, 8, 4, 2, 1};
        for (int nClient : nClients) {
            try (ChronicleContext context = ChronicleContext.newContext(URL)) {
                EchoHandler echoHandler = new EchoHandler();
                final ChronicleChannelSupplier supplier = context.newChannelSupplier(echoHandler);

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

        int size = TopOfBook.LENGTH_BYTES;
        for (int t = 1; t <= 3; t++) {
            long start = System.currentTimeMillis();
            long end = start + RUN_TIME * 1000L;
            int window = 8 * bufferSize / (4 + size) / nClients;
            AtomicLong totalRead = new AtomicLong(0);
            final Consumer<InternalChronicleChannel> sendAndReceive;

            // send messages via MethodWriters
            sendAndReceive = icc -> {
                int written = 0;
                final TopOfBookListener echoing = icc.methodWriter(TopOfBookListener.class);
                long[] read = {0};
                final MethodReader reader = icc.methodReader((TopOfBookListener) m -> read[0]++);
                TopOfBook tob = new TopOfBook();
                do {
                    echoing.topOfBook(tob);

                    written++;

                    readUpto(window, reader, written, read);
                } while (System.currentTimeMillis() < end);

                readUpto(0, reader, written, read);
                totalRead.addAndGet(read[0]);
            };
            Stream.of(clients)
                    .parallel()
                    .forEach(sendAndReceive);

            long count = totalRead.get();
            long time = System.currentTimeMillis() - start;
            long totalBytes = size * count;
            double GBps = (totalBytes + totalBytes) / (time / 1e3) / 1e9;
            long rate = (count + count) * 1000 / time;
            System.out.printf("clients; %d; desc; %s; size; %,6d; GB/s; %6.3f; Mmsg/s; %6.3f%n",
                    nClients, desc, size, GBps, rate / 1e6);
        }
    }

    private static void readUpto(int window, MethodReader reader, long written, long[] read) {
        do {
            reader.readOne();
        } while (written - read[0] > window);
    }
}
