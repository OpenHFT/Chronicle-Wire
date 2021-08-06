package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.time.SetTimeProvider;
import org.junit.Assert;
import org.junit.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static net.openhft.chronicle.wire.Marshallable.fromString;

public class DoubleTest {

    static class TwoDoubleDto extends SelfDescribingMarshallable {
        double price;
        double qty;
    }

    @Test
    public void testParsingForTwoDoubles() {
        CLASS_ALIASES.addAlias(TwoDoubleDto.class);
        final String EXPECTED = "!TwoDoubleDto {\n" +
                "price: 43298.21,\n" +
                "qty: 0.2886,\n" +
                "}\n";
        Assert.assertEquals(EXPECTED, fromString(TwoDoubleDto.class, EXPECTED).toString());
    }

}
