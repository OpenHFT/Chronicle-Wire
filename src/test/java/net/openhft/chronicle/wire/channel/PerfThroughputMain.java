/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.channel.echo.DummyData;
import net.openhft.chronicle.wire.channel.echo.EchoHandler;

import java.util.concurrent.atomic.AtomicLong;

public class PerfThroughputMain {
    static final int RUN_TIME = Integer.getInteger("runTime", 5);
    static final String URL = System.getProperty("url", "tcp://:1248");

    public static void main(String[] args) {
        try (ChronicleContext context = ChronicleContext.newContext(URL)) {
            EchoHandler echoHandler = new EchoHandler();
            final ChronicleChannelSupplier supplier = context.newChannelSupplier(echoHandler);
            ChronicleChannel channel = supplier.get();

            doTest("unbuffered", channel);

            echoHandler.buffered(true);
            ChronicleChannel channel2 = supplier.buffered(true).get();

            doTest("buffered", channel2);
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
            MethodReader reader = channel.methodReader((Echoing) d -> {
                unread.getAndDecrement();
            });
            int window = (128 << 20) / size;
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
            long time = System.currentTimeMillis() - start;
            long totalBytes = size * count;
            long MBps = totalBytes / time / (1_000_000 / 1_000);
            long rate = count * 1000 / time;
            System.out.println("desc: " + desc + ", size: " + size + ", MBps: " + MBps+", mps: "+rate);
        }
    }
}
