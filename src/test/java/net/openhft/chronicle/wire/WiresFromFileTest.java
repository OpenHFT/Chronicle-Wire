/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
public class WiresFromFileTest extends WireTestCommon {
    @Test
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
                "]", Arrays.asList(o).toString());
    }

    @Test
    public void testStreamFromFile() throws IOException {
        // Add an alias for MDU class
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);

        // Stream content from md2.yaml, extract the symbol from each MDU object, and collect them into a list
        List<String> symbols = Marshallable.streamFromFile(MDU.class, "md2.yaml")
                .map(m -> m.symbol)
                .collect(Collectors.toList());

        // Validate the extracted symbols
        assertEquals("[EU, UY, AU]", symbols.toString());
    }

    // Definition for MDU class
    static class MDU extends SelfDescribingMarshallable {
        String symbol;
    }
}
