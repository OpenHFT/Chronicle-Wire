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

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

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
