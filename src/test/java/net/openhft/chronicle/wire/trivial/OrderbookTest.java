package net.openhft.chronicle.wire.trivial;

import net.openhft.chronicle.wire.Marshallable;
import org.junit.Assert;
import org.junit.Test;

public class OrderbookTest {

    @Test
    public void test() {

        String expected = "!net.openhft.chronicle.wire.trivial.Orderbook {\n" +
                "  eventTime: \"2023-08-23T11:49:04.308\",\n" +
                "  symbol: EUR/USD,\n" +
                "  exchange: \"\",\n" +
                "  bid: [\n" +
                "    { price: 91.0, qty: 500.0 },\n" +
                "    { price: 90.0, qty: 1.0 }\n" +
                "  ],\n" +
                "  ask: [\n" +
                "    { price: 92.0, qty: 500.0 },\n" +
                "    { price: 93.0, qty: 1.0 },\n" +
                "    { price: 93.0, qty: 1.0 }\n" +
                "  ]\n" +
                "}\n";
        Object o = Marshallable.fromString(expected);

        Assert.assertEquals(expected, o.toString());
    }
}
