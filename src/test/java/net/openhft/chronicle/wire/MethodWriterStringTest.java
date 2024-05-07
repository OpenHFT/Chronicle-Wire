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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class tests the behavior of MethodWriter when handling String messages.
 * It extends the WireTestCommon from the `net.openhft.chronicle.wire` package for common test setup and utilities.
 */
public class MethodWriterStringTest extends net.openhft.chronicle.wire.WireTestCommon {
    // A blocking queue to hold String messages, used for synchronization between writer and reader.
    private ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(1);

    /**
     * An interface defining a single method that accepts a String message.
     */
    interface Print {
        void msg(String message);
    }

    /**
     * This test verifies that a String message can be written and read using MethodWriter and MethodReader respectively.
     */
    @Test
    public void test() throws InterruptedException {
        // Initialization of the wire
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        Print printer = w.methodWriter(Print.class);
        printer.msg("hello");

        // Set up a MethodReader to read the String message and process it using the println method
        MethodReader reader = w.methodReader((Print) this::println);
        reader.readOne();

        // Fetch the read message from the blocking queue with a timeout
        String result = q.poll(10, TimeUnit.SECONDS);
        // Verify that the fetched message matches the expected content
        Assert.assertEquals("hello", result);
    }

    /**
     * A helper method to add String messages to the blocking queue.
     */
    private void println(String msg) {
        q.add(msg);
    }
}
