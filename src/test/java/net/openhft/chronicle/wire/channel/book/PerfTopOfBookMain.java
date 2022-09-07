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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.threads.ThreadDump;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.channel.ChronicleChannelSupplier;
import net.openhft.chronicle.wire.channel.ChronicleContext;
import net.openhft.chronicle.wire.channel.InternalChronicleChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/*
On a Ryzen 5950X, bare metal Ubuntu 21.10

Azul Zulu 17.0.4.1
clients; 16; desc; buffered; size;     44; GB/s;  1.618; Mmsg/s; 36.783
clients; 8; desc; buffered; size;     44; GB/s;  1.170; Mmsg/s; 26.600
clients; 4; desc; buffered; size;     44; GB/s;  0.745; Mmsg/s; 16.928
clients; 2; desc; buffered; size;     44; GB/s;  0.392; Mmsg/s;  8.908
clients; 1; desc; buffered; size;     44; GB/s;  0.206; Mmsg/s;  4.682

--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-exports=java.base/jdk.internal.util=ALL-UNNAMED
--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED

-DoneNewObject=false -Xmn512m -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.485; Mmsg/s; 34.116
0 GCs/min

-DoneNewObject=true -Xmn24g -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.302; Mmsg/s; 29.595
6 GCs/min

-DoneNewObject=true -Xmn2g -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.290; Mmsg/s; 29.324
49 GCs/min

-DoneNewObject=true -Xmn1g -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.264; Mmsg/s; 28.724
102 GCs/min

-DoneNewObject=true -Xmn512m -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.239; Mmsg/s; 28.162
232 GCs/min
 */
public class PerfTopOfBookMain {
    static final String URL = System.getProperty("url", "tcp://:1248");
    static final int RUN_TIME = Integer.getInteger("runTime", 60);
    static final int CLIENTS = Integer.getInteger("clients", 0);
    static boolean ONE__NEW_OBJECT = Jvm.getBoolean("oneNewObject");
    static volatile TopOfBook blackHole;

    static {
        System.setProperty("system.properties", "/dev/null");
        System.setProperty("useAffinity", "false");
        System.setProperty("pauserMode", "yielding");
    }

    public static void main(String[] args) {
        System.out.println("-Durl=" + URL + " " +
                "-DrunTime=" + RUN_TIME + " " +
                "-Dclients=" + CLIENTS + " " +
                "-DoneNewObject=" + ONE__NEW_OBJECT
        );
        System.out.println("This is the total of the messages sent and messages received");
        int[] nClients = {CLIENTS};
        if (CLIENTS == 0)
            nClients = new int[]{16, 8, 4, 2, 1};
        warmUp();
        System.gc();
        TopOfBookHandler echoHandler = new TopOfBookHandler(new EchoTopOfBookHandler());
        for (int nClient : nClients) {
            ThreadDump td = new ThreadDump();
            try (ChronicleContext context = ChronicleContext.newContext(URL)) {
                final ChronicleChannelSupplier supplier = context.newChannelSupplier(echoHandler);

                echoHandler.buffered(true);
                doTest("buffered", supplier.buffered(true), nClient);
            }
            // check everything has shutdown, this is just for testing purposes.
            td.assertNoNewThreads();
        }
    }

    private static void warmUp() {
        Wire wire = WireType.BINARY_LIGHT.apply(Bytes.elasticByteBuffer());
        final TopOfBookListener writer = wire.methodWriter(TopOfBookListener.class);
        final TopOfBook topOfBook = new TopOfBook();
        long[] read = {0};
        TopOfBookListener tobl = topOfBookCounter(read);
        final MethodReader reader = wire.methodReader(tobl);
        for (int i = 0; i < 12_000; i++) {
            writer.topOfBook(topOfBook);
            reader.readOne();
            if (i % 256 == 0)
                wire.clear();
        }
    }

    private static void doTest(String desc, ChronicleChannelSupplier channelSupplier, int nClients) {
        InternalChronicleChannel[] clients = new InternalChronicleChannel[nClients];
        for (int i = 0; i < nClients; i++)
            clients[i] = (InternalChronicleChannel) channelSupplier.get();
        int bufferSize = clients[0].bufferSize();
        if (bufferSize < 4 << 20 && OS.isLinux()) {
            System.err.println("Try increasing the maximum buffer sizes");
            System.err.println("sudo cpupower frequency-set -g performance");
            System.err.println("sudo sysctl --write net.core.rmem_max=4194304");
            System.err.println("sudo sysctl --write net.core.wmem_max=4194304");
        }

        int size = TopOfBook.LENGTH_BYTES;
        for (int t = 1; t <= 2; t++) {
            long start = System.currentTimeMillis();
            long end = start + RUN_TIME * 1000L;
            int window = 4 * bufferSize / (4 + size) / nClients;
            AtomicLong totalRead = new AtomicLong(0);
            final Consumer<InternalChronicleChannel> sendAndReceive;

            // send messages via MethodWriters
            sendAndReceive = icc -> {
                int written = 0;
                final TopOfBookListener echoing = icc.methodWriter(TopOfBookListener.class);
                long[] read = {0};
                TopOfBookListener tobl = topOfBookCounter(read);
                TopOfBookListener topOfBookListener;
                if (ONE__NEW_OBJECT) {
                    topOfBookListener = m -> {
                        blackHole = m.deepCopy();
                        tobl.topOfBook(m);
                    };
                } else {
                    topOfBookListener = tobl;
                }
                final MethodReader reader = icc.methodReader(topOfBookListener);
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

    @NotNull
    private static TopOfBookListener topOfBookCounter(long[] read) {
        return m -> read[0]++;
    }

    private static void readUpto(int window, MethodReader reader, long written, long[] read) {
        do {
            reader.readOne();
        } while (written - read[0] > window);
    }
}
