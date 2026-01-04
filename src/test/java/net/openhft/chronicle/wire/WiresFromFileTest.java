/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
public class WiresFromFileTest extends WireTestCommon {
    @Test
    @DisplayName("File deserialisation returns expected MDU list")
    public void testFromFile() throws IOException {
        // Add an alias for MDU class
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);

        // Deserialize the content of the md.yaml file to an array of MDU objects
        MDU[] o = Marshallable.fromFile(MDU[].class, "md.yaml");

        // Validate the deserialized content
        assertEquals("[!MDU {\n" +
                "  symbol: EU\n" +
                "}\n" +
                ", !MDU {\n" +
                "  symbol: UY\n" +
                "}\n" +
                ", !MDU {\n" +
                "  symbol: AU\n" +
                "}\n" +
                "]",
                Arrays.asList(o).toString(),
                "MDU list should match expected file contents");
    }

    @Test
    @DisplayName("Stream from file yields expected symbols")
    public void testStreamFromFile() throws IOException {
        // Add an alias for MDU class
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);

        // Stream content from md2.yaml, extract the symbol from each MDU object, and collect them into a list
        List<String> symbols = Marshallable.streamFromFile(MDU.class, "md2.yaml")
                .map(m -> m.symbol)
                .collect(Collectors.toList());

        // Validate the extracted symbols
        assertEquals("[EU, UY, AU]",
                symbols.toString(),
                "Symbol list should match expected file contents");
    }

    // Definition for MDU class
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class MDU extends SelfDescribingMarshallable {
        String symbol;
    }
}
