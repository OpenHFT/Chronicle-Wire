package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.YamlWire;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Issue272Test {
    @Test
    public void arrays() {
        Bytes<?> buffer = Bytes.allocateElasticOnHeap();
        int[] one = {1, 2, 3, 4, 5};
        int[] two = {101, 102, 103, 104, 105};

        // Create wire for saving two int arrays
        Wire wire = new YamlWire(buffer);
        wire.write("one").array(one, 5);
        wire.write("two").array(two, 5);
        System.out.println("Write wire:");
        System.out.println(wire);

        // read the original wire
        doTest(wire);

        // Read the original buffer, but use a fresh YamlWire
        Wire wire2 = new YamlWire(buffer.readPosition(0));
        doTest(wire2);

        // copy the original bytes
        Wire wire3 = new YamlWire(buffer.readPosition(0).copy().bytesForRead());
        doTest(wire3);

        // translate the data to binary wire and back again
        HexDumpBytes bytes = new HexDumpBytes();
        Wire copyWire0 = new BinaryWire(bytes);
        new YamlWire(buffer.readPosition(0))
                .copyTo(copyWire0);
        assertEquals("" +
                        "b9 03 6f 6e 65                                  # one: (event)\n" +
                        "82 0a 00 00 00                                  # sequence\n" +
                        "a1 01                                           # 1\n" +
                        "a1 02                                           # 2\n" +
                        "a1 03                                           # 3\n" +
                        "a1 04                                           # 4\n" +
                        "a1 05                                           # 5\n" +
                        "b9 03 74 77 6f                                  # two: (event)\n" +
                        "82 0a 00 00 00                                  # sequence\n" +
                        "a1 65                                           # 101\n" +
                        "a1 66                                           # 102\n" +
                        "a1 67                                           # 103\n" +
                        "a1 68                                           # 104\n" +
                        "a1 69                                           # 105\n",
                bytes.toHexString());

        Wire copyWire = new YamlWire(Bytes.allocateElasticOnHeap());
        copyWire0.copyTo(copyWire);
        System.out.println(copyWire);
        doTest(copyWire);
    }

    @Test
    public void map() {
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

        HexDumpBytes bytes = new HexDumpBytes();
        Wire copyWire = new BinaryWire(bytes);
        copyAll(wire, copyWire);
        assertEquals("" +
                        "1b 00 00 00                                     # msg-length\n" +
                        "82 16 00 00 00                                  # Marshallable\n" +
                        "c2 74 6f                                        # to:\n" +
                        "e5 64 65 73 74 31                               # dest1\n" +
                        "c4 73 65 6e 64                                  # send:\n" +
                        "e7 6d 65 73 73 61 67 65                         # message\n" +
                        "25 00 00 00                                     # msg-length\n" +
                        "82 20 00 00 00                                  # Marshallable\n" +
                        "c2 74 6f                                        # to:\n" +
                        "e5 64 65 73 74 32                               # dest2\n" +
                        "c5 73 65 6e 64 73                               # sends:\n" +
                        "82 0c 00 00 00                                  # Marshallable\n" +
                        "c3 6f 6e 65                                     # one:\n" +
                        "a1 01                                           # 1\n" +
                        "c3 74 77 6f                                     # two:\n" +
                        "a1 02                                           # 2\n" +
                        "00 00 00 00                                     # msg-length\n",
                bytes.toHexString());

        Wire wire2 = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        copyAll(copyWire, wire2);
        assertEquals("to: dest1\n" +
                        "send: message\n" +
                        "...\n" +
                        "{\n" +
                        "  to: dest2,\n" +
                        "  sends: {\n" +
                        "    one: 1,\n" +
                        "    two: 2\n" +
                        "  }\n" +
                        "}\n" +
                        "...\n" +
                        "0\n" +
                        "0\n" +
                        "0\n" +
                        "0\n" +
                        "...\n",
                wire2.toString());
    }

    private static void copyAll(Wire wire, Wire copyWire) {
        while(!wire.isEmpty()) {
            try (DocumentContext dc = wire.readingDocument()) {
                try (DocumentContext dc2 = copyWire.writingDocument()) {
                    wire.copyTo(copyWire);
                }
            }
        }
    }

    private static void doTest(Wire copyWire) {
        int[] readBuffer = new int[5];
        int amtRead = copyWire.read("one").array(readBuffer);
        assertEquals("5, [1, 2, 3, 4, 5]", amtRead + ", " + Arrays.toString(readBuffer));
        int amtRead2 = copyWire.read("two").array(readBuffer);
        assertEquals("5, [101, 102, 103, 104, 105]", amtRead2 + ", " + Arrays.toString(readBuffer));
    }
}
