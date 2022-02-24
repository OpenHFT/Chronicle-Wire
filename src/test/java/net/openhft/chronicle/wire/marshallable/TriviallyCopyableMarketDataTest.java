package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class TriviallyCopyableMarketDataTest {
    @Test
    public void test() {
        assumeFalse(Jvm.isAzulZing());
        ClassAliasPool.CLASS_ALIASES.addAlias(TriviallyCopyableMarketData.class, "MarketData");
        final String str = "" +
                "!MarketData {\n" +
                "  securityId: EUR/GBP,\n" +
                "  time: 2021-12-07T17:00:47.626128,\n" +
                "  bid0: 0.84963,\n" +
                "  bid1: 0.84964,\n" +
                "  bid2: 0.84965,\n" +
                "  bid3: 0.84967,\n" +
                "  bidQty0: 1E6,\n" +
                "  bidQty1: 2E6,\n" +
                "  bidQty2: 3E6,\n" +
                "  bidQty3: 4E6,\n" +
                "  ask0: 0.84961,\n" +
                "  ask1: 0.8496,\n" +
                "  ask2: 0.84959,\n" +
                "  ask3: 0.84958,\n" +
                "  askQty0: 1E6,\n" +
                "  askQty1: 3E6,\n" +
                "  askQty2: 5E6,\n" +
                "  askQty3: 2E6\n" +
                "}\n";
        TriviallyCopyableMarketData data = Marshallable.fromString(str);

        Bytes bytes = Bytes.allocateElasticDirect();
        data.writeMarshallable(bytes);

        TriviallyCopyableMarketData data2 = new TriviallyCopyableMarketData();
        data2.readMarshallable(bytes);

        assertEquals(str, data.toString());
        assertEquals(str, data2.toString());
        assertEquals(data, data2);
    }
}