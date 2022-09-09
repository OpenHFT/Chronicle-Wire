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

import net.openhft.affinity.Affinity;
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

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/*
On a Ryzen 5950X, bare metal Ubuntu 21.10

Azul Zulu 18.0.2.1
-Durl=tcp://:1248 -DrunTime=60 -Dclients=0 -DoneNewObject=false

clients; 16; desc; buffered; size;     44; GB/s;  2.846; Mmsg/s; 64.690
clients; 8; desc; buffered; size;     44; GB/s;  2.023; Mmsg/s; 45.976
clients; 4; desc; buffered; size;     44; GB/s;  1.419; Mmsg/s; 32.261
clients; 2; desc; buffered; size;     44; GB/s;  0.915; Mmsg/s; 20.793
clients; 1; desc; buffered; size;     44; GB/s;  0.460; Mmsg/s; 10.447

0 GCs/min

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

-Durl=tcp://:1248 -DrunTime=60 -Dclients=16 -DoneNewObject=false
clients; 16; desc; buffered; size;     44; GB/s;  2.875; Mmsg/s; 65.344 - Azul Zulu 11.0.14.1

-Durl=tcp://localhost:1248 -DrunTime=60 -Dclients=16 -DoneNewObject=false
clients; 16; desc; buffered; size;     44; GB/s;  2.665; Mmsg/s; 60.578 - Azul Zulu 1.8.0_322
clients; 16; desc; buffered; size;     44; GB/s;  2.961; Mmsg/s; 67.290 - Azul Zulu 11.0.14.1
clients; 16; desc; buffered; size;     44; GB/s;  3.020; Mmsg/s; 68.628 - Azul Zulu 17.0.4.1
clients; 16; desc; buffered; size;     44; GB/s;  2.972; Mmsg/s; 67.540 - Azul Zulu 18.0.2.1
clients; 16; desc; buffered; size;     44; GB/s;  2.984; Mmsg/s; 67.811 - Oracle OpenJDK 18.0.2.1

-Durl=tcp://localhost:1248 -DrunTime=60 -Dclients=16 -DoneNewObject=true
clients; 16; desc; buffered; size;     44; GB/s;  1.601; Mmsg/s; 36.384 - Azul Zulu 1.8.0_322
clients; 16; desc; buffered; size;     44; GB/s;  2.011; Mmsg/s; 45.710 - Azul Zulu 11.0.14.1
clients; 16; desc; buffered; size;     44; GB/s;  2.215; Mmsg/s; 50.341 - Azul Zulu 17.0.4.1
clients; 16; desc; buffered; size;     44; GB/s;  2.192; Mmsg/s; 49.818 - Azul Zulu 18.0.2.1
clients; 16; desc; buffered; size;     44; GB/s;  2.205; Mmsg/s; 50.118 - Oracle OpenJDK 18.0.2.1

-DoneNewObject=true -Xmn16g -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.728; Mmsg/s; 39.271 - Corretto 11.0.14.1
clients; 16; desc; buffered; size;     44; GB/s;  1.760; Mmsg/s; 39.991 - Oracle 18.0.2.1
clients; 16; desc; buffered; size;     44; GB/s;  1.799; Mmsg/s; 40.891 - Azul Zulu 11.0.14.1
clients; 16; desc; buffered; size;     44; GB/s;  1.761; Mmsg/s; 40.012 - Oracle 17.0.2
clients; 16; desc; buffered; size;     44; GB/s;  1.823; Mmsg/s; 41.428 - Azul Zulu 17.0.4.1
8 GCs/min < 30 ms/min

-DoneNewObject=true -Xmn2g -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.718; Mmsg/s; 39.042
62 GCs/min < 40 ms/min

-DoneNewObject=true -Xmn1g -Dclients=16
clients; 16; desc; buffered; size;     44; GB/s;  1.703; Mmsg/s; 38.698
123 GCs/min

-DoneNewObject=true -Dclients=16 (348M young size)
clients; 16; desc; buffered; size;     44; GB/s;  1.635; Mmsg/s; 37.151
345 GCs/min
 */
public class PerfTopOfBookMain {
    static {
        System.setProperty("system.properties", "/dev/null");
        System.setProperty("useAffinity", "false");
        System.setProperty("pauserMode", "yielding");
    }
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
            if (nClient <= 2)
                Affinity.setAffinity(BitSet.valueOf(new byte[] { -1 }));

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
