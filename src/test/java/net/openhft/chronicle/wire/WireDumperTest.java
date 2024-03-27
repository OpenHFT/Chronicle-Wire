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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class WireDumperTest extends WireTestCommon {

    // Instance variables for the test setup and expected outputs
    private final Bytes<?> bytes;
    private final Wire wire;
    private final WireType wireType;
    private final Map<WireType, String> expectedContentByType = new HashMap<>();
    private final Map<WireType, String> expectedPartialContent = new HashMap<>();

    // Constructor to set up the test environment based on a WireType
    public WireDumperTest(final String name, final WireType wireType) {
        bytes = Bytes.allocateElasticOnHeap(); // Allocate elastic bytes
        wire = wireType.apply(bytes);          // Create a wire based on the given WireType
        wire.usePadding(true);

        this.wireType = wireType;
        initTestData();  // Populate the expected outputs
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return toParams(WireType.values());
    }

    // Helper method to filter and format WireType values for parameterized testing
    private static Object[][] toParams(final WireType[] values) {
        return Arrays.stream(values).filter(WireType::isAvailable)
                .filter(wt -> wt != WireType.CSV)
                .filter(wt -> wt != WireType.READ_ANY)
                .filter(wt -> wt != WireType.JSON_ONLY)
                .filter(wt -> wt != WireType.YAML_ONLY)
                .map(wt -> new Object[]{wt.toString(), wt})
                .toArray(Object[][]::new);
    }

    // Test case for verifying serialization of content to a wire
    @Test
    public void shouldSerialiseContent() {
        // Writing values to the wire
        wire.writeDocument(17L, ValueOut::int64);
        wire.writeDocument("bark", ValueOut::text);
        wire.writeDocument(3.14D, ValueOut::float64);

        final String actual = isText(wire.bytes()) ? wire.toString() : WireDumper.of(wire).asString();
        assertEquals(expectedContentByType.get(wireType), actual);  // Asserting expected vs actual content
    }

    // Helper method to check if the given bytes represent text
    private boolean isText(Bytes<?> bytes) {
        for (int i = 0; i < 8 && i < bytes.readRemaining(); i++) {
            final int ch = bytes.peekUnsignedByte(bytes.readPosition() + i);
            if (Character.isWhitespace(ch) || (' ' < ch && ch < 127))
                continue;
            return false;
        }
        return true;
    }

    // Test case for verifying serialization of partial content to a wire
    @Test
    public void shouldSerialisePartialContent() {
        // Writing partial content to the wire
        wire.writeDocument(17L, ValueOut::int64);
        final DocumentContext context = wire.writingDocument();
        context.wire().getValueOut().text("meow");

        final String actual = isText(wire.bytes()) ? wire.toString() : WireDumper.of(wire).asString();
        assertEquals(expectedPartialContent.get(wireType), actual);  // Asserting expected vs actual partial content
    }

    // Overridden method to perform clean up after each test case
    @Override
    public void preAfter() {
        bytes.releaseLast();  // Releasing the last bytes used
    }

    private void initTestData() {

        // Adding the expected serialized output for TEXT WireType
        expectedContentByType.put(WireType.TEXT, "" +
                "--- !!data\n" +
                "17\n" +
                "# position: 8, header: 1\n" +
                "--- !!data\n" +
                "bark\n" +
                "# position: 20, header: 2\n" +
                "--- !!data\n" +
                "3.14\n" +
                "");

        // Adding expected serialized content for YAML WireType
        expectedContentByType.put(WireType.YAML, "" +
                "--- !!data\n" +
                "17\n" +
                "# position: 8, header: 1\n" +
                "--- !!data\n" +
                "bark\n" +
                "# position: 20, header: 2\n" +
                "--- !!data\n" +
                "3.14\n" +
                "");

        // Using a common string to represent the expected binary content for several WireTypes
        final String expectedBinary = "" +
                "--- !!data #binary\n" +
                "17\n" +
                "# position: 8, header: 1\n" +
                "--- !!data #binary\n" +
                "bark\n" +
                "# position: 20, header: 2\n" +
                "--- !!data #binary\n" +
                "3.14\n";

        // Setting the expected output for various WireTypes using the above defined expectedBinary
        expectedContentByType.put(WireType.BINARY, expectedBinary);

        expectedContentByType.put(WireType.BINARY_LIGHT, expectedBinary);

        expectedContentByType.put(WireType.FIELDLESS_BINARY, expectedBinary);

        expectedContentByType.put(WireType.COMPRESSED_BINARY, expectedBinary);

        expectedContentByType.put(WireType.JSON, "" +
                "--- !!data\n" +
                "17\n" +
                "# position: 8, header: 1\n" +
                "--- !!data\n" +
                "\"bark\"\n" +
                "# position: 20, header: 2\n" +
                "--- !!data\n" +
                "3.14\n" +
                "");
        expectedContentByType.put(WireType.JSON_ONLY, "" +
                "17\n" +
                "\"bark\"\n" +
                "3.14\n");
        expectedContentByType.put(WireType.RAW, "" +
                "--- !!data #binary\n" +
                "00000000             11 00 00 00  00 00 00 00                 ···· ····    \n" +
                "# position: 12, header: 1\n" +
                "--- !!data #binary\n" +
                "00000010 04 62 61 72 6b                                   ·bark            \n" +
                "# position: 24, header: 2\n" +
                "--- !!data #binary\n" +
                "00000010                                      1f 85 eb 51              ···Q\n" +
                "00000020 b8 1e 09 40                                      ···@             \n");

        expectedContentByType.put(WireType.YAML_ONLY, "" +
                "17\n" +
                "...\n" +
                "bark\n" +
                "...\n" +
                "3.14\n" +
                "...\n");

        // Setting the expected partial serialized content for different WireTypes
        expectedPartialContent.put(WireType.TEXT, "" +
                "--- !!data\n" +
                "17\n" +
                "# position: 8, header: 0 or 1\n" +
                "--- !!not-ready-data\n" +
                "...\n" +
                "# 5 bytes remaining\n" +
                "");
        expectedPartialContent.put(WireType.YAML, "" +
                "--- !!data\n" +
                "17\n" +
                "# position: 8, header: 0 or 1\n" +
                "--- !!not-ready-data\n" +
                "...\n" +
                "# 5 bytes remaining\n" +
                "");
        final String expectedPartialBinary = "" +
                "--- !!data #binary\n" +
                "17\n" +
                "# position: 8, header: 0 or 1\n" +
                "--- !!not-ready-data\n" +
                "...\n" +
                "# 5 bytes remaining\n";

        // Setting the expected partial output for various WireTypes using the above defined expectedPartialBinary
        expectedPartialContent.put(WireType.BINARY, expectedPartialBinary);

        expectedPartialContent.put(WireType.BINARY_LIGHT, expectedPartialBinary);

        expectedPartialContent.put(WireType.FIELDLESS_BINARY, expectedPartialBinary);

        expectedPartialContent.put(WireType.COMPRESSED_BINARY, expectedPartialBinary);

        expectedPartialContent.put(WireType.JSON, "" +
                "--- !!data\n" +
                "17\n" +
                "# position: 8, header: 0 or 1\n" +
                "--- !!not-ready-data\n" +
                "...\n" +
                "# 6 bytes remaining\n" +
                "");

        expectedPartialContent.put(WireType.JSON_ONLY, "" +
                "17\n" +
                "\"meow\"");

        expectedPartialContent.put(WireType.RAW, "" +
                "--- !!data #binary\n" +
                "00000000             11 00 00 00  00 00 00 00                 ···· ····    \n" +
                "# position: 12, header: 0 or 1\n" +
                "--- !!not-ready-data\n" +
                "...\n" +
                "# 5 bytes remaining\n" +
                "");

        expectedPartialContent.put(WireType.YAML_ONLY, "" +
                "17\n" +
                "...\n" +
                "meow\n");

    }
}
