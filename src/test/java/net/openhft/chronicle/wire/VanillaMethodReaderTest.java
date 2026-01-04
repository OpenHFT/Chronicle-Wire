/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.marshallable.TriviallyCopyableMarketData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Extend WireTestCommon to inherit common utility and setup methods for wire tests
class VanillaMethodReaderTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Define an interface representing a method with a single message parameter
    public interface MyMethod {
        void msg(String str);
    }

    // Test case to check the behavior of a predicate that always returns false
    @Test
    @DisplayName("Predicate false prevents any method dispatch")
    void testPredicateFalse() {

        // Allocate elastic bytes on heap and create a TextWire instance
        Bytes<byte[]> b = Bytes.allocateElasticOnHeap();
        Wire w = new TextWire(b);

        // Create a method writer for the MyMethod interface and send a message
        MyMethod build1 = w.methodWriterBuilder(MyMethod.class).build();
        build1.msg("hi");

        // Prepare an array to capture the method's output
        final String[] value = new String[1];

        // Build a method reader with a predicate that always returns false
        MethodReader reader = new VanillaMethodReaderBuilder(w)
                .predicate(x -> false)
                .build((MyMethod) str -> value[0] = str);

        // Assert that no message was read and the value remains null
        Assertions.assertFalse(reader.readOne(), "Predicate false should suppress all reads");
        Assertions.assertNull(value[0], "No message should be delivered when predicate fails");
    }

    // Test case to check the behavior of a predicate that always returns true
    @Test
    @DisplayName("Predicate true allows single method dispatch")
    void testPredicateTrue() {

        // Allocate elastic bytes on heap and create a TextWire instance
        Bytes<byte[]> b = Bytes.allocateElasticOnHeap();
        Wire w = new TextWire(b);

        // Create a method writer for the MyMethod interface and send a message
        MyMethod build1 = w.methodWriterBuilder(MyMethod.class)
                .build();
        build1.msg("hi");

        // Initialize a method reader builder with a predicate that always returns true
        VanillaMethodReaderBuilder builder = new VanillaMethodReaderBuilder(w);
        builder.predicate(x -> true);

        // Prepare an array to capture the method's output
        final String[] value = new String[1];

        // Build the method reader and assert that the message was read correctly
        MethodReader reader = builder.build((MyMethod) str -> value[0] = str);

        Assertions.assertTrue(reader.readOne(), "Predicate true should allow one read");
        Assertions.assertEquals("hi", value[0], "Reader should deliver the hi payload");
    }

    // Test case to log a binary message and validate its content
    @Test
    @DisplayName("Binary log message matches expected hex dump")
    void logMessage0() {

        // do not check Mac as it lays it memory out differently
        Assumptions.assumeTrue(!OS.isMacOSX(),
                "macOS layout differs for hex dump expectations");

        TriviallyCopyableMarketData data = new TriviallyCopyableMarketData();
        data.securityId(0x828282828282L);

        // Write the market data to a binary light wire
        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        wire.methodWriter(ITCO.class).marketData(data);

        // Assert that the binary representation of the message matches the expected output
        assertEquals("9e 00 00 00                                     # msg-length\n" +
                        "b9 0a 6d 61 72 6b 65 74 44 61 74 61             # marketData: (event)\n" +
                        "80 90 82 82 82 82 82 82 00 00 00 00 00 00 00 00 # TriviallyCopyableMarketData\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00\n",
                wire.bytes().toHexString(),
                "Binary wire hex dump should match expected output");

        // Read the written message and validate its content
        try (DocumentContext dc = wire.readingDocument()) {
            final ValueIn marketData = dc.wire().read("marketData");

            assertEquals("read md - 00000010 80 90 82 82 82 82 82 82  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "........\n" +
                            "000000a0 00 00                                            ··               ",
                    VanillaMethodReader.logMessage0("md", marketData),
                    "Log message should include expected market data dump");
        }
    }

    // Define an interface representing a method to handle market data
    interface ITCO {
        void marketData(TriviallyCopyableMarketData tcmd);
    }
}
