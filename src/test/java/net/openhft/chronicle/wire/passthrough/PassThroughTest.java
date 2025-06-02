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

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Test class extending WireTestCommon to validate the pass-through functionality of method writers
 * and readers in Chronicle Wire using different wire formats.
 */
public class PassThroughTest extends WireTestCommon {

    /**
     * Tests the pass-through functionality using TextWire, ensuring that the method reader can
     * read and pass through the input text correctly.
     */
    @Test
    public void testPassThroughputText() {
        String input = "" +
                "to: dest1\n" +
                "send: message\n" +
                "...\n" +
                "to: dest2\n" +
                "sends: {\n" +
                "  one: 1,\n" +
                "  two: 2\n" +
                "}\n" +
                "...\n";
        Wire wire = new TextWire(Bytes.from(input)).useTextDocuments();
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        final DestinationSpecific destinationSpecific = wire2.methodWriter(DestinationSpecific.class);
        final MethodReader reader = wire.methodReader(destinationSpecific);

        // Read and assert each document in the wire
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());

        // Assert the output of wire2 matches the input
        assertEquals(input, wire2.toString());

        // Setup another wire for reading the output of wire2 and assert the result
        Bytes<?> bytes3 = new HexDumpBytes();
        Wire wire3 = new TextWire(bytes3).useTextDocuments();

        final MethodReader reader2 = wire2.methodReader(new Destination() {
            @Override
            public DocumentContext to(String dest) {
                final DocumentContext dc = wire3.writingDocument();
                dc.wire().write("to").text(dest);
                return dc;
            }
        });
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader2.readOne());
        assertEquals(input, wire3.toString());
    }

    /**
     * Tests the method writer functionality using TextWire, ensuring that the written document
     * matches the expected format.
     */
    @Test
    public void methodWriterText() {
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        final Destination destination = wire2.methodWriter(Destination.class);
        try (DocumentContext dc = destination.to("dest")) {
            dc.wire().write("send").text("message");
        }

        // Assert the content written to wire2
        assertEquals("" +
                        "to: dest\n" +
                        "send: message\n" +
                        "...\n",
                wire2.toString());
    }

    /**
     * Tests the pass-through functionality using YamlWire, ensuring that the method reader can
     * read and pass through the input YAML text correctly.
     */
    @Test
    public void testPassThroughputYaml() {
        String input = "" +
                "to: dest1\n" +
                "send: message\n" +
                "...\n" +
                "to: dest2\n" +
                "sends: {\n" +
                "  one: 1,\n" +
                "  two: 2\n" +
                "}\n" +
                "...\n";
        Wire wire = new YamlWire(Bytes.from(input)).useTextDocuments();
        Wire wire2 = Wire.newYamlWireOnHeap();
        final DestinationSpecific destinationSpecific = wire2.methodWriter(DestinationSpecific.class);
        final MethodReader reader = wire.methodReader(destinationSpecific);

        // Read and assert each document in the wire
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());

        // Assert the output of wire2 matches the input
        assertEquals(input, wire2.toString());

        // Setup another wire for reading the output of wire2 and assert the result
        Bytes<?> bytes3 = new HexDumpBytes();
        Wire wire3 = new YamlWire(bytes3).useTextDocuments();

        final MethodReader reader2 = wire2.methodReader(new Destination() {
            @Override
            public DocumentContext to(String dest) {
                final DocumentContext dc = wire3.writingDocument();
                dc.wire().write("to").text(dest);
                return dc;
            }
        });
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader2.readOne());
        assertEquals(input, wire3.toString());
    }

    /**
     * Tests the method writer functionality using YamlWire, ensuring that the written document
     * matches the expected YAML format.
     */
    @Test
    public void methodWriterYaml() {
        // Create a YAML wire for writing
        Wire wire2 = Wire.newYamlWireOnHeap();
        final Destination destination = wire2.methodWriter(Destination.class);

        // Write to the wire using the Destination interface
        try (DocumentContext dc = destination.to("dest")) {
            dc.wire().write("send").text("message");
        }

        // Assert the content written to wire2
        assertEquals("" +
                        "to: dest\n" +
                        "send: message\n" +
                        "...\n",
                wire2.toString());
    }

    /**
     * Tests the pass-through functionality using BinaryWire, ensuring that the method reader can
     * read and pass through binary formatted messages correctly.
     */
    @Test
    public void testPassThroughputBinary() {
        String input = "" +
                "to: dest1\n" +
                "send: message\n" +
                "...\n" +
                "to: dest2\n" +
                "sends: {\n" +
                "  one: 1,\n" +
                "  two: 2\n" +
                "}\n" +
                "...\n";
        // Create a YAML wire from the input string
        Wire wire = new YamlWire(Bytes.from(input)).useTextDocuments();
        // Create a binary wire for pass-through
        final HexDumpBytes hdb = new HexDumpBytes();
        Wire wire2 = WireType.BINARY_LIGHT.apply(hdb);
        final DestinationSpecific destinationSpecific = wire2.methodWriter(DestinationSpecific.class);

        // Read from the YAML wire and write to the binary wire
        final MethodReader reader = wire.methodReader(destinationSpecific);
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());

        // Assert the binary output
        assertEquals("" +
                        "18 00 00 00                                     # msg-length\n" +
                        "b9 02 74 6f                                     # to: (event)\n" +
                        "e5 64 65 73 74 31                               # dest1\n" +
                        "b9 04 73 65 6e 64                               # send: (event)\n" +
                        "e7 6d 65 73 73 61 67 65                         # message\n" +
                        "32 00 00 00                                     # msg-length\n" +
                        "b9 02 74 6f                                     # to: (event)\n" +
                        "e5 64 65 73 74 32                               # dest2\n" +
                        "b9 05 73 65 6e 64 73                            # sends: (event)\n" +
                        "82 1c 00 00 00                                  # MapMarshaller\n" +
                        "b9 03 6f 6e 65                                  # one: (event)\n" +
                        "a7 01 00 00 00 00 00 00 00                      # 1\n" +
                        "b9 03 74 77 6f                                  # two: (event)\n" +
                        "a7 02 00 00 00 00 00 00 00                      # 2\n",
                wire2.bytes().toHexString());

        // Setup another binary wire for reading the output of wire2 and assert the result
        Bytes<?> bytes3 = new HexDumpBytes();
        Wire wire3 = WireType.BINARY_LIGHT.apply(bytes3);
        wire3.usePadding(false);

        final MethodReader reader2 = wire2.methodReader(new Destination() {
            @Override
            public DocumentContext to(String dest) {
                final DocumentContext dc = wire3.writingDocument();
                dc.wire().write("to").text(dest);
                return dc;
            }
        });
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader2.readOne());
        assertEquals("" +
                        "17 00 00 00                                     # msg-length\n" +
                        "c2 74 6f                                        # to:\n" +
                        "e5 64 65 73 74 31                               # dest1\n" +
                        "b9 04 73 65 6e 64 e7 6d 65 73 73 61 67 65       # passed-through\n" +
                        "31 00 00 00                                     # msg-length\n" +
                        "c2 74 6f                                        # to:\n" +
                        "e5 64 65 73 74 32                               # dest2\n" +
                        "b9 05 73 65 6e 64 73 82 1c 00 00 00 b9 03 6f 6e # passed-through\n" +
                        "65 a7 01 00 00 00 00 00 00 00 b9 03 74 77 6f a7\n" +
                        "02 00 00 00 00 00 00 00\n",
                wire3.bytes().toHexString().replaceAll("Lambda.*", "Lambda"));

        // Setup a text wire for reading the output of wire3 and compare it to the original input
        Wire wire4 = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        final DestinationSpecific destinationSpecific4 = wire4.methodWriter(DestinationSpecific.class);
        final MethodReader reader3 = wire3.methodReader(destinationSpecific4);
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader3.readOne());

        assertEquals(input, wire4.toString());

        // Release resources
        bytes3.releaseLast();
        hdb.releaseLast();
    }

    /**
     * Tests the method writer functionality using BinaryWire, ensuring that the written document
     * matches the expected binary format.
     */
    @Test
    public void methodWriterBinary() {
        // Create a binary wire for writing
        Wire wire2 = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        final Destination destination = wire2.methodWriter(Destination.class);

        // Write to the wire using the Destination interface
        try (DocumentContext dc = destination.to("dest")) {
            dc.wire().write("send").text("message");
        }

        // Assert the content written to wire2 in binary format
        assertEquals("" +
                        "16 00 00 00                                     # msg-length\n" +
                        "b9 02 74 6f                                     # to: (event)\n" +
                        "e4 64 65 73 74                                  # dest\n" +
                        "c4 73 65 6e 64                                  # send:\n" +
                        "e7 6d 65 73 73 61 67 65                         # message\n",
                wire2.bytes().toHexString());
    }

    /**
     * Interface representing a destination with a method to route actions to a specific destination.
     */
    interface Destination {
        /**
         * Routes the action to the specified destination.
         *
         * @param dest The destination name.
         * @return DocumentContext representing the context of the document being written.
         */
        DocumentContext to(String dest);
    }

    /**
     * Interface for sending messages in different formats.
     */
    interface Sending {
        /**
         * Sends a simple text message.
         *
         * @param msg The message to be sent.
         */
        void send(String msg);

        /**
         * Sends a message with a map of string to integer values.
         *
         * @param map The map to be sent as a message.
         */
        void sends(Map<String, Integer> map);
    }

    /**
     * Interface combining the functionality of sending messages to a specific destination.
     */
    interface DestinationSpecific {
        /**
         * Routes the action of sending to the specified destination.
         *
         * @param dest The destination name.
         * @return Sending interface for further action.
         */
        Sending to(String dest);
    }
}
