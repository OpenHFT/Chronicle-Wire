/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class BracketsOnJSONWireTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Variable to store the actual message from the wire
    String actual;

    // Interface to define a Printer with a single method 'print'
    interface Printer {
        void print(String msg);
    }

    // Test the JSON_ONLY wire type with a method writer and reader using the Printer interface
    @Test
    public void test() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Create an elastic byte buffer to hold the wire data
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();

        // Initialize the wire with JSON_ONLY type and apply it to the buffer
        Wire wire = WireType.JSON_ONLY.apply(t);

        // Use a method writer to write a print message to the wire
        wire.methodWriter(Printer.class)
                .print("hello");

        // Assert that the wire representation matches the expected JSON format
        assertEquals("{\"print\":\"hello\"}", wire.toString());

        // Use a method reader to read the message from the wire and set the 'actual' variable
        wire.methodReader((Printer) msg -> actual = msg).readOne();

        // Release the buffer to free up resources
        t.releaseLast();

        // Assert that the read message matches the original message written to the wire
        assertEquals("hello", actual);
    }
}
