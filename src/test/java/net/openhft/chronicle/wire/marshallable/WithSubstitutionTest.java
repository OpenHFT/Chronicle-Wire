package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class WithSubstitutionTest extends WireTestCommon {
    @Test
    public void subs() {
        expectException("Cannot read ${num} as a number, treating as 0");
        expectException("Cannot read ${num2} as a number, treating as 0");
        expectException("Cannot read ${d} as a number, treating as 0");
        expectException("Cannot read ${d2} as a number, treating as 0");
        expectException("Found an unsubstituted ${} as ${text");
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
        assertEquals("[!WSDTO {\n" +
                "  num: 0,\n" +
                "  d: 0.0,\n" +
                "  text: \"${text}\"\n" +
                "}\n" +
                ", !WSDTO {\n" +
                "  num: 0,\n" +
                "  d: 0.0,\n" +
                "  text: \"${text2}\"\n" +
                "}\n" +
                "]", wsdtos.toString());
    }

    static class WSDTO extends SelfDescribingMarshallable {
        int num;
        double d;
        String text;
    }
}
