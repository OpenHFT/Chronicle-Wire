//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class tests the behavior of MethodWriter when handling Bytes as input.
 * It extends the WireTestCommon from the `net.openhft.chronicle.wire` package for common test setup and utilities.
 */
@SuppressWarnings("rawtypes")
public class MethodWriterBytesTest extends net.openhft.chronicle.wire.WireTestCommon {
    // A blocking queue to hold Bytes instances, used for synchronization between writer and reader.
    private ArrayBlockingQueue<Bytes> q = new ArrayBlockingQueue<>(1);

    /**
     * An interface defining a single method that accepts a Bytes message.
     */
    interface Print {
        void msg(Bytes<?> message);
    }

    /**
     * This test verifies that a Bytes message can be written and read using MethodWriter and MethodReader respectively.
     */
    @Test
    public void test() throws InterruptedException {
        // Initialization of the wire
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        Print printer = w.methodWriter(Print.class);
        printer.msg(Bytes.from("hello"));

        // Set up a MethodReader to read the Bytes message and process it using the println method
        MethodReader reader = w.methodReader((Print) this::println);
        reader.readOne();

        // Fetch the read message from the blocking queue with a timeout
        Bytes result = q.poll(10, TimeUnit.SECONDS);
        // Verify that the fetched message matches the expected content
        Assert.assertEquals("hello", result.toString());
        if (result != null) {
            result.releaseLast();
        }
        w.bytes().releaseLast();
    }

    /**
     * A helper method to add Bytes messages to the blocking queue.
     */
    private void println(Bytes<?> bytes) {
        q.add(bytes);
    }

    @Test
    public void reusedBytesRemainStableAcrossDispatches() throws InterruptedException {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        Print printer = wire.methodWriter(Print.class);
        Bytes<?> reusable = Bytes.allocateElasticOnHeap();
        try {
            reusable.writeUtf8("alpha");
            printer.msg(reusable);

            reusable.clear();
            reusable.writeUtf8("beta");
            printer.msg(reusable);

            ArrayBlockingQueue<String> sink = new ArrayBlockingQueue<>(2);
            MethodReader reader = wire.methodReader((Print) bytes -> {
                bytes.readPosition(0);
                sink.add(bytes.readUtf8());
            });

            Assert.assertTrue(reader.readOne());
            Assert.assertEquals("alpha", sink.poll(5, TimeUnit.SECONDS));
            Assert.assertTrue(reader.readOne());
            Assert.assertEquals("beta", sink.poll(5, TimeUnit.SECONDS));
        } finally {
            reusable.releaseLast();
            wire.bytes().releaseLast();
        }
    }

    @Test
    public void producerMutationDuringCallbackDoesNotCorruptPayload() throws InterruptedException {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        Bytes<?> shared = Bytes.allocateElasticOnHeap();
        try {
            Print printer = wire.methodWriter(Print.class);
            shared.writeUtf8("original");
            printer.msg(shared);

            ArrayBlockingQueue<String> sink = new ArrayBlockingQueue<>(1);
            MethodReader reader = wire.methodReader((Print) bytes -> {
                // mutate the shared Bytes while the reader is consuming
                shared.clear();
                shared.writeUtf8("mutated");
                bytes.readPosition(0);
                sink.add(bytes.readUtf8());
            });

            Assert.assertTrue(reader.readOne());
            Assert.assertEquals("original", sink.poll(5, TimeUnit.SECONDS));
        } finally {
            shared.releaseLast();
            wire.bytes().releaseLast();
        }
    }
}
