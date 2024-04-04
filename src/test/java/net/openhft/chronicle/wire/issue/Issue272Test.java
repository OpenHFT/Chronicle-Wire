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

/**
 * Test class to validate the behavior and serialization of Wire objects with arrays.
 */
public class Issue272Test {

    /**
     * Test case to handle the serialization and deserialization of int arrays
     * using Wire. This test also verifies the conversion between YamlWire
     * and BinaryWire and their respective outputs.
     */
    @Test
    public void arrays() {
        // Allocate a buffer to hold the serialized data
        Bytes<?> buffer = Bytes.allocateElasticOnHeap();

        // Define two int arrays for testing
        int[] one = {1, 2, 3, 4, 5};
        int[] two = {101, 102, 103, 104, 105};

        // Initialize a YamlWire and write the two int arrays into it
        Wire wire = new YamlWire(buffer);
        wire.write("one").array(one, 5);
        wire.write("two").array(two, 5);
        System.out.println("Write wire:");
        System.out.println(wire);

        // Verify the contents of the wire by reading it
        doTest(wire);

        // Create a fresh YamlWire instance and read the original buffer
        Wire wire2 = new YamlWire(buffer.readPosition(0));
        doTest(wire2);

        // Create a fresh YamlWire instance using a copy of the original bytes
        Wire wire3 = new YamlWire(buffer.readPosition(0).copy().bytesForRead());
        doTest(wire3);

        // Create a BinaryWire instance and translate data from the original YamlWire
        HexDumpBytes bytes = new HexDumpBytes();
        Wire copyWire0 = new BinaryWire(bytes);
        new YamlWire(buffer.readPosition(0)) // Verify the hex representation of the binary wire
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

        // Create a fresh YamlWire, copy data from the BinaryWire, and verify the result
        Wire copyWire = new YamlWire(Bytes.allocateElasticOnHeap());
        copyWire0.copyTo(copyWire);
        System.out.println(copyWire);
        doTest(copyWire);
    }

    /**
     * Test case to handle the serialization and deserialization of map structures
     * using Wire. This test verifies the conversion between YamlWire
     * and BinaryWire and their respective outputs for map structures.
     */
    @Test
    public void map() {
        // Create an input string with Yaml format to be parsed
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

        // Initialize a YamlWire from the input string and set it to use text documents
        Wire wire = new YamlWire(Bytes.from(input)).useTextDocuments();

        // Convert the YamlWire into a BinaryWire and verify the hex representation
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
                        "a1 02                                           # 2\n",
                bytes.toHexString());

        // Convert the BinaryWire back into a YamlWire and verify its string representation
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
                        "...\n",
                wire2.toString());
    }

    /**
     * Copies all documents from a source Wire to a destination Wire.
     * This method ensures that the entire content is replicated accurately.
     *
     * @param wire     The source Wire to be copied from.
     * @param copyWire The destination Wire to be copied to.
     */
    private static void copyAll(Wire wire, Wire copyWire) {
        // Iterate through all documents in the source wire
        while(!wire.isEmpty()) {
            try (DocumentContext dc = wire.readingDocument()) {
                try (DocumentContext dc2 = copyWire.writingDocument()) {
                    dc.wire().copyTo(dc2.wire());
                }
            }
        }
    }

    /**
     * Validates the contents of a Wire that has two arrays "one" and "two".
     * This method checks if the Wire correctly read the serialized arrays.
     *
     * @param copyWire The Wire object to be tested.
     */
    private static void doTest(Wire copyWire) {
        // Create a buffer to read the contents of the arrays
        int[] readBuffer = new int[5];

        // Read the "one" array and validate its contents
        int amtRead = copyWire.read("one").array(readBuffer);
        assertEquals("5, [1, 2, 3, 4, 5]", amtRead + ", " + Arrays.toString(readBuffer));

        // Read the "two" array and validate its contents
        int amtRead2 = copyWire.read("two").array(readBuffer);
        assertEquals("5, [101, 102, 103, 104, 105]", amtRead2 + ", " + Arrays.toString(readBuffer));
    }
}
