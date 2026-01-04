/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class tests the behavior of MethodWriter when handling Bytes as input.
 * It extends the WireTestCommon from the `net.openhft.chronicle.wire` package for common test setup and utilities.
 */
@SuppressWarnings("rawtypes")
class MethodWriterBytesTest extends net.openhft.chronicle.wire.WireTestCommon {
    // A blocking queue to hold Bytes instances, used for synchronization between writer and reader.
    private final ArrayBlockingQueue<Bytes> q = new ArrayBlockingQueue<>(1);

    /**
     * This test verifies that a Bytes message can be written and read using MethodWriter and MethodReader respectively.
     */
    @Test
    @DisplayName("Bytes payload can be written and read")
    void test() throws InterruptedException {
        // Initialization of the wire
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        Print printer = w.methodWriter(Print.class);
        printer.msg(Bytes.from("hello"));

        // Set up a MethodReader to read the Bytes message and process it using the println method
        MethodReader reader = w.methodReader((Print) this::println);
        Assertions.assertTrue(reader.readOne(), "Reader should process the Bytes message");

        // Fetch the read message from the blocking queue with a timeout
        Bytes result = q.poll(10, TimeUnit.SECONDS);
        Assertions.assertNotNull(result, "Reader should supply a Bytes payload");
        Assertions.assertEquals("hello", result.toString(),
                "Payload should match the written Bytes content");
        result.releaseLast();
        w.bytes().releaseLast();
    }

    /**
     * A helper method to add Bytes messages to the blocking queue.
     */
    private void println(Bytes<?> bytes) {
        q.add(bytes);
    }

    @Test
    @DisplayName("Reused Bytes remain stable across dispatches")
    void reusedBytesRemainStableAcrossDispatches() throws InterruptedException {
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

            Assertions.assertTrue(reader.readOne(), "Reader should process the first message");
            Assertions.assertEquals("alpha", sink.poll(5, TimeUnit.SECONDS),
                    "First payload should be alpha");
            Assertions.assertTrue(reader.readOne(), "Reader should process the second message");
            Assertions.assertEquals("beta", sink.poll(5, TimeUnit.SECONDS),
                    "Second payload should be beta");
        } finally {
            reusable.releaseLast();
            wire.bytes().releaseLast();
        }
    }

    @Test
    @DisplayName("Producer mutation during callback does not corrupt payload")
    void producerMutationDuringCallbackDoesNotCorruptPayload() throws InterruptedException {
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

            Assertions.assertTrue(reader.readOne(), "Reader should process the shared Bytes message");
            Assertions.assertEquals("original", sink.poll(5, TimeUnit.SECONDS),
                    "Payload should remain original despite producer mutation");
        } finally {
            shared.releaseLast();
            wire.bytes().releaseLast();
        }
    }

    /**
     * An interface defining a single method that accepts a Bytes message.
     */
    interface Print {
        void msg(Bytes<?> message);
    }
}
