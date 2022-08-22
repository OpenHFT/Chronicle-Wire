/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.channel.echo.DummyData;
import net.openhft.chronicle.wire.channel.echo.EchoHandler;

import java.util.concurrent.atomic.AtomicLong;

public class PerfThroughputMain {
    static final String URL = System.getProperty("url", "tcp://:1248");
    static final int RUN_TIME = Integer.getInteger("runTime", 5);
    static final boolean METHOD_RW = Jvm.getBoolean("method.rw");

    public static void main(String[] args) {
        System.out.println("-Durl=" + URL + " " +
                "-DrunTime=" + RUN_TIME + " " +
                "-Dmethod.rw=" + METHOD_RW);
        try (ChronicleContext context = ChronicleContext.newContext(URL)) {
            EchoHandler echoHandler = new EchoHandler();
            final ChronicleChannelSupplier supplier = context.newChannelSupplier(echoHandler);
            echoHandler.buffered(true);
            try (ChronicleChannel channel2 = supplier.buffered(true).get()) {
                doTest("buffered", channel2);
            }

            echoHandler.buffered(false);
            try (ChronicleChannel channel = supplier.get()) {
                doTest("unbuffered", channel);
            }
        }
    }

    private static void doTest(String desc, ChronicleChannel channel) {
        DummyData data = new DummyData();
        for (int size = 32 << 10; size >= 8; size /= 2) {
            data.data(new byte[size - Long.BYTES]);
            long start = System.currentTimeMillis();
            long end = start + RUN_TIME * 1000L;
            final Echoing echoing = channel.methodWriter(Echoing.class);
            long count = 0;
            AtomicLong unread = new AtomicLong(0);
            int window = (128 << 20) / size;
            if (METHOD_RW) {
                MethodReader reader = channel.methodReader((Echoing) d -> {
                    unread.getAndDecrement();
                });
                do {
                    echoing.echo(data);
                    unread.getAndIncrement();
                    count++;
                    do {
                        reader.readOne();
                    } while (unread.get() > window);
                } while (System.currentTimeMillis() < end);

                do {
                    reader.readOne();
                } while (unread.get() > 0);

            } else {
                InternalChronicleChannel icc = (InternalChronicleChannel) channel;
                do {
                    final Bytes<?> bytes = icc.acquireProducer().bytes();
                    bytes.writeInt(size);
                    for (int i = 0; i < size; i += 8)
                        bytes.writeLong(0L);
                    icc.releaseProducer();

                    unread.getAndIncrement();
                    count++;
                    do {
                        try (DocumentContext dc = channel.readingDocument()) {
                            if (dc.isPresent())
                                unread.getAndDecrement();
                        }
                    } while (unread.get() > window);
                } while (System.currentTimeMillis() < end);

                do {
                    try (DocumentContext dc = channel.readingDocument()) {
                        if (dc.isPresent())
                            unread.getAndDecrement();
                    }
                } while (unread.get() > 0);
            }
            long time = System.currentTimeMillis() - start;
            long totalBytes = size * count;
            long MBps = totalBytes / time / (1_000_000 / 1_000);
            long rate = count * 1000 / time;
            System.out.println("desc: " + desc + ", size: " + size + ", MBps: " + MBps + ", mps: " + rate);
        }
    }
}
