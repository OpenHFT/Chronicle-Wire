/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
    void test() throws InterruptedException {
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
        assertEquals("hello", result.toString());
    }

    /**
     * A helper method to add Bytes messages to the blocking queue.
     */
    private void println(Bytes<?> bytes) {
        q.add(bytes);
    }
}
