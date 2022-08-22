/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.channel.echo.EchoNHandler;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class PerfThroughputMain {
    static final String URL = System.getProperty("url", "tcp://:1248");
    static final int RUN_TIME = Integer.getInteger("runTime", 5);
    static final int BATCH = Integer.getInteger("batch", 1);
    static final int CLIENTS = Integer.getInteger("clients", 1);

    public static void main(String[] args) {
        System.out.println("-Durl=" + URL + " " +
                "-DrunTime=" + RUN_TIME + " " +
                "-Dclients=" + CLIENTS + " " +
                "-Dbatch=" + BATCH
        );
        try (ChronicleContext context = ChronicleContext.newContext(URL)) {
            EchoNHandler echoHandler = new EchoNHandler();
            echoHandler.times(BATCH);
            final ChronicleChannelSupplier supplier = context.newChannelSupplier(echoHandler);
            echoHandler.buffered(true);
            doTest("buffered", supplier.buffered(true));

            echoHandler.buffered(false);
            doTest("unbuffered", supplier.buffered(false));
        }
    }

    private static void doTest(String desc, ChronicleChannelSupplier channelSupplier) {
        InternalChronicleChannel[] clients = new InternalChronicleChannel[CLIENTS];
        for (int i = 0; i < CLIENTS; i++)
            clients[i] = (InternalChronicleChannel) channelSupplier.get();

        for (int size = 32 << 10; size >= 8; size /= 2) {
            long start = System.currentTimeMillis();
            long end = start + RUN_TIME * 1000L;
            int window = (16 << 20) / size / CLIENTS;
            AtomicLong totalRead = new AtomicLong(0);
            int finalSize = size;
            Stream.of(clients)
                    .parallel()
                    .forEach(icc -> {
                        int written = 0, read = 0;

                        do {
                            final Bytes<?> bytes = icc.acquireProducer().bytes();
                            bytes.writeInt(finalSize);
                            for (int i = 0; i < finalSize; i += 8)
                                bytes.writeLong(0L);
                            icc.releaseProducer();

                            // due to the multiplier in the EchoNHandler
                            written += BATCH;

                            read = readUpto(window, icc, written, read);
                        } while (System.currentTimeMillis() < end);

                        readUpto(0, icc, written, read);
                        totalRead.addAndGet(read);
                    });

            long count = totalRead.get();
            long time = System.currentTimeMillis() - start;
            long totalBytes = size * count;
            long MBps = totalBytes / time / (1_000_000 / 1_000);
            long rate = count * 1000 / time;
            System.out.printf("desc: %s, size: %,d, MBps: %,d, mps: %,d%n",
                    desc, size, MBps, rate);
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
