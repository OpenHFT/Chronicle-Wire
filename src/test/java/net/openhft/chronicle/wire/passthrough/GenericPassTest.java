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

package net.openhft.chronicle.wire.passthrough;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class extending WireTestCommon to validate the behavior of method readers and writers
 * in Chronicle Wire, specifically focusing on passing messages through brokers.
 */
public class GenericPassTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Tests the functionality of a saying broker by writing and reading messages through Chronicle Wire.
     * It verifies that messages are correctly passed and received via a broker.
     */
    @Test
    public void sayingBroker() {
        // Create a wire for writing a message
        Wire wire1 = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        final SayingBroker sayingBroker = wire1.methodWriter(SayingBroker.class);
        sayingBroker.via("queue1").say("hello");

        // Assert the contents written to wire1
        assertEquals("" +
                        "via: queue1\n" +
                        "say: hello\n" +
                        "...\n",
                wire1.toString());

        // Setup wire2 for reading the message
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        final DocumentContextBroker microService = new MyDocumentContextBroker(wire2);

        // Read the message using a method reader and assert the behavior
        final MethodReader reader = wire1.methodReader(microService);
        assertTrue(reader.readOne()); // Read one message
        assertFalse(reader.readOne()); // Assert no more messages
        assertEquals("" +
                        "via: queue1\n" +
                        "say: hello\n" +
                        "...\n",
                wire2.toString());
    }

    /**
     * Tests the passing of an opaque message (one that is not directly interpretable) through Chronicle Wire.
     * It verifies that such messages can be passed through the wire without interpretation or modification.
     */
    @Test
    public void passingOpaqueMessage() {
        // Create a wire with an invalid byte sequence to simulate an opaque message
        Bytes<?> bytes0 = Bytes.allocateElasticOnHeap(1);
        bytes0.writeUnsignedByte(0x82);  // Invalid byte in every wire type
        Wire wire0 = new TextWire(bytes0).useTextDocuments();

        // Attempt to read the opaque message and expect an exception
        try {
            wire0.getValueIn().object();
            fail(); // Fail if no exception is thrown
        } catch (Exception ise) {
            // Expected exception caught
        }

        // Setup wire1 for writing the opaque message
        Wire wire1 = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        wire1.write("via").text("pass");
        wire1.bytes().writeUnsignedByte(0x82); // Write the opaque byte

        // Setup wire2 for reading the message
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        final DocumentContextBroker microService = new MyDocumentContextBroker(wire2);

        // Read the message using a method reader and assert the behavior
        final MethodReader reader = wire1.methodReader(microService);
        assertTrue(reader.readOne()); // Read one message
        assertFalse(reader.readOne()); // Assert no more messages
        // the ... is added by the wire format.
        assertEquals("" +
                        "via: pass\n" +
                        "\u0082\n" +
                        "...\n",
                wire2.toString());
    }

    /**
     * Tests the passing of an opaque message (one that is not directly interpretable) through BinaryWire.
     * It verifies that such messages can be passed through the wire without interpretation or modification.
     */
    @Test
    public void passingOpaqueMessageBinary() {
        // Create a wire with an invalid byte sequence to simulate an opaque message
        Bytes<?> bytes0 = Bytes.allocateElasticOnHeap(1);
        bytes0.writeUnsignedByte(0x82); // Invalid byte in binary wire format
        Wire wire0 = new BinaryWire(bytes0);

        // Attempt to read the opaque message and expect an exception
        try {
            wire0.getValueIn().object();
            fail(); // Fail if no exception is thrown
        } catch (Exception ise) {
            // Expected exception caught
        }

        // Setup wire1 for writing the opaque message
        Wire wire1 = new BinaryWire(new HexDumpBytes());
        final DocumentContextBroker dcb = wire1.methodWriter(DocumentContextBroker.class);
        try (DocumentContext dc = dcb.via("pass")) {
            dc.wire().bytes()
                    .writeHexDumpDescription("opaque message")
                    .writeUnsignedByte(0x82);
        }

        // Assert the written data in wire1
        assertEquals("" +
                        "0b 00 00 00                                     # msg-length\n" +
                        "b9 03 76 69 61                                  # via: (event)\n" +
                        "e4 70 61 73 73                                  # pass\n" +
                        "82                                              # opaque message\n",
                wire1.bytes().toHexString());

        // Setup wire2 for reading the message
        Wire wire2 = new BinaryWire(new HexDumpBytes());
        final DocumentContextBroker microService = new MyDocumentContextBroker(wire2);

        // Read the message using a method reader and assert the behavior
        final MethodReader reader = wire1.methodReader(microService);
        assertTrue(reader.readOne()); // Read one message
        assertFalse(reader.readOne()); // Assert no more messages
        assertEquals("" +
                        "0b 00 00 00                                     # msg-length\n" +
                        "b9 03 76 69 61                                  # via: (event)\n" +
                        "e4 70 61 73 73                                  # pass\n" +
                        "82                                              # passed-through\n",
                wire2.bytes().toHexString());
    }

    /**
     * Interface representing a broker with a method to route actions via a named path.
     * @param <T> The type of action or behavior being routed.
     */
    interface Broker<T> {
        T via(String name);
    }

    /**
     * Interface for routing actions using an alternative method.
     * @param <T> The type of action or behavior being routed.
     */
    interface Another<T> {
        T also(String name);
    }

    /**
     * Combined interface for a broker handling both document contexts and saying actions.
     */
    interface DocumentContextBroker extends Broker<DocumentContext>, Another<Saying> {

    }

    /**
     * Interface for saying actions.
     */
    interface Saying {
        void say(String msg);
    }

    /**
     * Interface for a broker handling saying actions.
     */
    interface SayingBroker extends Broker<Saying> {

    }

    /**
     * Implementation of DocumentContextBroker, providing the logic for handling document contexts.
     */
    private static class MyDocumentContextBroker implements DocumentContextBroker {
        private final Wire wire2; // Wire to write messages to

        public MyDocumentContextBroker(Wire wire2) {
            this.wire2 = wire2;
        }

        @Override
        public DocumentContext via(String name) {
            // Write the event to the wire
            final DocumentContext dc = wire2.writingDocument();
            dc.wire().writeEvent(String.class, "via").text(name);
            return dc;
        }

        @Override
        public Saying also(String name) {
            return null; // No implementation for this method
        }
    }
}
