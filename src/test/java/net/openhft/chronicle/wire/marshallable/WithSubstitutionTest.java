package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import org.junit.Test;

import java.util.List;

public class WithSubstitutionTest {
    @Test
    public void subs() {
        ClassAliasPool.CLASS_ALIASES.addAlias(WSDTO.class);
        List<WSDTO> wsdtos = Marshallable.fromString(
                "[\n" +
                        "  !WSDTO {\n" +
                        "    num: ${num},\n" +
                        "    d: ${d}\n" +
                        "    text: ${text}\n" +
                        "  },\n" +
                        "  !WSDTO {\n" +
                        "    num: ${num2},\n" +
                        "    text: ${text2}\n" +
                        "    d: ${d2}\n" +
                        "  }\n" +
                        "]\n");
        System.out.println(wsdtos);
    }

    static class WSDTO extends SelfDescribingMarshallable {
        int num;
        double d;
        String text;
    }
}
