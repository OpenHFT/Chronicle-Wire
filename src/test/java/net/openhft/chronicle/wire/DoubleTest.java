package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static net.openhft.chronicle.wire.Marshallable.fromString;

public class DoubleTest {

    static class TwoDoubleDto extends SelfDescribingMarshallable {
        double price;
        double qty;
    }

    /**
     * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/299 Fixed case where a serializable 'double' value sometimes has trailing zero
     */
    @Test
    public void testParsingForTwoDoubles() {
        CLASS_ALIASES.addAlias(TwoDoubleDto.class);
        final String EXPECTED = "!TwoDoubleDto {\n" +
                "  price: 43298.21,\n" +
                "  qty: 0.2886\n" +
                "}\n";
        final TwoDoubleDto twoDoubleDto = fromString(TwoDoubleDto.class, EXPECTED);

        Assert.assertEquals(EXPECTED, twoDoubleDto.toString());
    }

}
