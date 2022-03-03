package net.openhft.chronicle.wire.passthrough;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/*
 * Test that a flow style method writer can return a DocumentWritten
 */
public class PassThroughTest extends WireTestCommon {
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
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());

        assertEquals(input, wire2.toString());

        Bytes bytes3 = new HexDumpBytes();
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

    @Test
    public void methodWriterText() {
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        final Destination destination = wire2.methodWriter(Destination.class);
        try (DocumentContext dc = destination.to("dest")) {
            dc.wire().write("send").text("message");
        }
        assertEquals("" +
                        "to: dest\n" +
                        "send: message\n" +
                        "...\n",
                wire2.toString());
    }

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
        Wire wire2 = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        final DestinationSpecific destinationSpecific = wire2.methodWriter(DestinationSpecific.class);
        final MethodReader reader = wire.methodReader(destinationSpecific);
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());

        assertEquals(input, wire2.toString());

        Bytes bytes3 = new HexDumpBytes();
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

    @Test
    public void methodWriterYaml() {
        Wire wire2 = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        final Destination destination = wire2.methodWriter(Destination.class);
        try (DocumentContext dc = destination.to("dest")) {
            dc.wire().write("send").text("message");
        }
        assertEquals("" +
                        "to: dest\n" +
                        "send: message\n" +
                        "...\n",
                wire2.toString());
    }

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
        Wire wire = new YamlWire(Bytes.from(input)).useTextDocuments();
        final HexDumpBytes hdb = new HexDumpBytes();
        Wire wire2 = WireType.BINARY_LIGHT.apply(hdb);
        final DestinationSpecific destinationSpecific = wire2.methodWriter(DestinationSpecific.class);
        final MethodReader reader = wire.methodReader(destinationSpecific);
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());

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

        Bytes bytes3 = new HexDumpBytes();
        Wire wire3 = WireType.BINARY_LIGHT.apply(bytes3);

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

        Wire wire4 = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        final DestinationSpecific destinationSpecific4 = wire4.methodWriter(DestinationSpecific.class);
        final MethodReader reader3 = wire3.methodReader(destinationSpecific4);
        for (int i = 2; i >= 0; i--)
            assertEquals(i > 0, reader3.readOne());

        assertEquals(input, wire4.toString());
        bytes3.releaseLast();
        hdb.releaseLast();
    }

    @Test
    public void methodWriterBinary() {
        Wire wire2 = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        final Destination destination = wire2.methodWriter(Destination.class);
        try (DocumentContext dc = destination.to("dest")) {
            dc.wire().write("send").text("message");
        }
        assertEquals("" +
                        "16 00 00 00                                     # msg-length\n" +
                        "b9 02 74 6f                                     # to: (event)\n" +
                        "e4 64 65 73 74                                  # dest\n" +
                        "c4 73 65 6e 64                                  # send:\n" +
                        "e7 6d 65 73 73 61 67 65                         # message\n",
                wire2.bytes().toHexString());
    }

    interface Destination {
        DocumentContext to(String dest);
    }

    interface Sending {
        void send(String msg);

        void sends(Map<String, Integer> map);
    }

    interface DestinationSpecific {
        Sending to(String dest);
    }
}
