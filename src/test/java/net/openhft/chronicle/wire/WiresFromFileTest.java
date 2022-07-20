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
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);
        MDU[] o = Marshallable.fromFile(MDU[].class, "md.yaml");
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
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);
        List<String> symbols = Marshallable.streamFromFile(MDU.class, "md2.yaml")
                .map(m -> m.symbol)
                .collect(Collectors.toList());
        assertEquals("[EU, UY, AU]", symbols.toString());
    }

    static class MDU extends SelfDescribingMarshallable {
        String symbol;
    }
}
