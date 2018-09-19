package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 08/05/2017.
 */
public class WiresFromFileTest {
    @Test
    public void testFromFile() throws IOException {
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);
        MDU[] o = Marshallable.fromFile(MDU[].class, "md.yaml");
        System.out.println(Arrays.asList(o));
    }

    @Test
    public void testStreamFromFile() throws IOException {
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);
        List<String> symbols = Marshallable.streamFromFile(MDU.class, "md2.yaml")
                .map(m -> m.symbol)
                .collect(Collectors.toList());
        assertEquals("[EU, UY, AU]", symbols.toString());
    }

    static class MDU extends AbstractMarshallable {
        String symbol;
    }
}
