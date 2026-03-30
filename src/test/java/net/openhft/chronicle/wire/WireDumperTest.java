/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WireDumperTest extends WireTestCommon {

    // Helper method to filter and format WireType values for parameterized testing
    public static Object[][] parameters() {
        return toParams(WireType.values());
    }

    private static Object[][] toParams(final WireType[] values) {
        return Arrays.stream(values).filter(WireType::isAvailable)
                .filter(wt -> wt != WireType.CSV)
                .filter(wt -> wt != WireType.READ_ANY)
                .filter(wt -> wt != WireType.JSON_ONLY)
                .filter(wt -> wt != WireType.YAML_ONLY)
                .map(wt -> new Object[]{wt.toString(), wt})
                .toArray(Object[][]::new);
    }

    private Bytes<?> bytes;
    private Wire wire;
    private WireType wireType;
    private final Map<WireType, String> expectedContentByType = new HashMap<>();
    private final Map<WireType, String> expectedPartialContent = new HashMap<>();

    private void initTest(String name, WireType wireType) {
        bytes = Bytes.allocateElasticOnHeap();
        wire = wireType.apply(bytes);
        wire.usePadding(true);
        this.wireType = wireType;
        initTestData();
    }

    // Test case for verifying serialization of content to a wire
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    void shouldSerialiseContent(String name, WireType wireType) {
        initTest(name, wireType);
        try {
            // Writing values to the wire
            wire.writeDocument(17L, ValueOut::int64);
            wire.writeDocument("bark", ValueOut::text);
            wire.writeDocument(3.14D, ValueOut::float64);

            final String actual = isText(wire.bytes()) ? wire.toString() : WireDumper.of(wire).asString();
            assertEquals(expectedContentByType.get(wireType), actual);  // Asserting expected vs actual content
        } finally {
            bytes.releaseLast();
        }
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
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    void shouldSerialisePartialContent(String name, WireType wireType) {
        initTest(name, wireType);
        try {
            // Writing partial content to the wire
            wire.writeDocument(17L, ValueOut::int64);
            final DocumentContext context = wire.writingDocument();
            context.wire().getValueOut().text("meow");

            final String actual = isText(wire.bytes()) ? wire.toString() : WireDumper.of(wire).asString();
            assertEquals(expectedPartialContent.get(wireType), actual);  // Asserting expected vs actual partial content
        } finally {
            bytes.releaseLast();
        }
    }

    @SuppressWarnings("deprecation")
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

        // JSONL wraps each document in braces and adds newline
        expectedContentByType.put(WireType.JSONL, "" +
                "{17}\n" +
                "{\"bark\"}\n" +
                "{3.14}\n");

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

        // JSONL partial content - second document not yet closed
        expectedPartialContent.put(WireType.JSONL, "" +
                "{17}\n" +
                "{\"meow\"");

    }
}
