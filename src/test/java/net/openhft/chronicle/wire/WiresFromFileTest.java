package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by peter on 08/05/2017.
 */
public class WiresFromFileTest {
    @Test
    public void testFromFile() throws IOException {
        ClassAliasPool.CLASS_ALIASES.addAlias(MDU.class);
        MDU[] o = Marshallable.fromFile(MDU[].class, "md.yaml");
        System.out.println(Arrays.asList(o));
    }

    static class MDU extends AbstractMarshallable {
        String symbol;
    }
}
