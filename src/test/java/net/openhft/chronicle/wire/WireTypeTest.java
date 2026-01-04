/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.Time;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
class WireTypeTest extends WireTestCommon {

    // Add alias for MarshallableFixture class for the test
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(MarshallableFixture.class);
    }

    // Test if the WireType enum is correctly identified by its name
    @Test
    @DisplayName("WireType values report the expected type name")
    void testNameFor() {
        // Add alias for WireType class
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class);

        // Iterate over each WireType and check if its name is "WireType"
        for (WireType wireType : WireType.values()) {
            assertEquals("WireType",
                    Wires.typeNameFor(wireType),
                    "WireType name should resolve for each enum value: " + wireType);
        }
    }

    // Test conversion from a MarshallableFixture object to String representations
    @Test
    @DisplayName("WireType formats fixtures to text and binary")
    void testAsString() {
        @NotNull MarshallableFixture tm = new MarshallableFixture();
        tm.setCount(1);
        tm.setName("name");

        // Test Text-based WireType
        assertEquals("!MarshallableFixture {\n" +
                "  name: name,\n" +
                "  count: 1\n" +
                "}\n",
                WireType.TEXT.asString(tm),
                "Text wire output should match expected fixture format");
        // Test Binary-based WireType
        assertEquals("00000000 b6 13 4d 61 72 73 68 61  6c 6c 61 62 6c 65 46 69 ··Marsha llableFi\n" +
                        "00000010 78 74 75 72 65 82 12 00  00 00 c4 6e 61 6d 65 e4 xture··· ···name·\n" +
                        "00000020 6e 61 6d 65 c5 63 6f 75  6e 74 a1 01             name·cou nt··    \n",
                WireType.BINARY.asString(tm),
                "Binary wire output should match expected fixture format");

        assertEquals("00000000 13 4d 61 72 73 68 61 6c  6c 61 62 6c 65 46 69 78 ·Marshal lableFix\n" +
                "00000010 74 75 72 65 09 00 00 00  04 6e 61 6d 65 01 00 00 ture···· ·name···\n" +
                "00000020 00                                               ·                \n",
                WireType.RAW.asString(tm),
                "Raw wire output should match expected fixture format");
    }

    // Test conversion from String representations to a MarshallableFixture object
    @Test
    @DisplayName("WireType parses fixtures from text and binary")
    void testFromString() {
        // Define the text representation
        @NotNull String asText = "!MarshallableFixture {\n" +
                "  name: name,\n" +
                "  count: 1\n" +
                "}\n";

        // Create a MarshallableFixture object
        @NotNull MarshallableFixture tm = new MarshallableFixture();
        tm.setCount(1);
        tm.setName("name");

        // Validate Text-based WireType
        assertEquals(tm,
                WireType.TEXT.fromString(asText),
                "Text wire should parse fixture from string");

        // Define the binary representation
        @NotNull String asBinary = "00000000 B6 13 4D 61 72 73 68 61  6C 6C 61 62 6C 65 46 69 ··Marsha llableFi\n" +
                "00000010 78 74 75 72 65 82 12 00  00 00 C4 6E 61 6D 65 E4 xture··· ···name·\n" +
                "00000020 6E 61 6D 65 C5 63 6F 75  6E 74 A1 01             name·cou nt··    \n";
        // Validate Binary-based WireType
        assertEquals(tm,
                WireType.BINARY.fromString(asBinary),
                "Binary wire should parse fixture from hex dump");
    }

    // Test WireType's ability to write and read from a file
    @Test
    @DisplayName("WireType round trips fixtures through files")
    void testFromFile() throws IOException {
        // Create a MarshallableFixture object
        @NotNull MarshallableFixture tm = new MarshallableFixture();
        tm.setCount(1);
        tm.setName("name");

        // Iterate over each WireType for file-based tests
        for (@NotNull WireType wt : WireType.values()) {
            // Skip unsupported WireTypes
            if (wt == WireType.RAW
                    || wt == WireType.READ_ANY
                    || wt == WireType.CSV)
                continue;

            // Create a temporary file
            @NotNull String tmp = OS.getTarget() + "/testFromFile-" + Time.uniqueId();

            // Write the MarshallableFixture object to the file
            wt.toFile(tmp, tm);

            // Read the object back from the file and validate
            @Nullable Object o;
            if (wt == WireType.JSON || wt == WireType.JSON_ONLY)
                o = wt.apply(BytesUtil.readFile(tmp)).getValueIn().object(MarshallableFixture.class);
            else
                o = wt.fromFile(tmp);

            assertEquals(tm,
                    o,
                    "File round trip should preserve fixture for " + wt);
        }
    }
}
