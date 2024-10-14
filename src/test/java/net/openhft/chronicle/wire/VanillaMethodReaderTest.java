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
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.marshallable.TriviallyCopyableMarketData;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Extend WireTestCommon to inherit common utility and setup methods for wire tests
public class VanillaMethodReaderTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Define an interface representing a method with a single message parameter
    public interface MyMethod {
        void msg(String str);
    }

    // Test case to check the behavior of a predicate that always returns false
    @Test
    public void testPredicateFalse() {

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
        Assert.assertFalse(reader.readOne());
        Assert.assertNull(value[0]);
    }

    // Test case to check the behavior of a predicate that always returns true
    @Test
    public void testPredicateTrue() {

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

        Assert.assertTrue(reader.readOne());
        Assert.assertEquals("hi", value[0]);
    }

    // Test case to log a binary message and validate its content
    @Test
    public void logMessage0() {

        // do not check Mac as it lays it memory out differently
        Assume.assumeTrue(!OS.isMacOSX());

        TriviallyCopyableMarketData data = new TriviallyCopyableMarketData();
        data.securityId(0x828282828282L);

        // Write the market data to a binary light wire
        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        wire.methodWriter(ITCO.class).marketData(data);

        // Assert that the binary representation of the message matches the expected output
        assertEquals("" +
                        "9e 00 00 00                                     # msg-length\n" +
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
                wire.bytes().toHexString());

        // Read the written message and validate its content
        try (DocumentContext dc = wire.readingDocument()) {
            final ValueIn marketData = dc.wire().read("marketData");

            assertEquals("" +
                            "read md - 00000010 80 90 82 82 82 82 82 82  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "........\n" +
                            "000000a0 00 00                                            ··               ",
                    VanillaMethodReader.logMessage0("md", marketData));
        }
    }

    // Define an interface representing a method to handle market data
    interface ITCO {
        void marketData(TriviallyCopyableMarketData tcmd);
    }
}
