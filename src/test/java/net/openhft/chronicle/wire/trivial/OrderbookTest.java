package net.openhft.chronicle.wire.trivial;

import net.openhft.chronicle.wire.Marshallable;
import org.junit.Assert;
import org.junit.Test;

public class OrderbookTest {

    @Test
    public void test() {

        String expected = "!net.openhft.chronicle.wire.trivial.Orderbook {\n" +
                "  eventTime: 2023-08-23T11:49:04.308,\n" +
                "  symbol: EUR/USD,\n" +
                "  bid: [\n" +
                "    { price: 91, qty: 500K },\n" +
                "    { price: 90, qty: 1M }\n" +
                "  ],\n" +
                "  ask: [\n" +
                "    { price: 92, qty: 500K },\n" +
                "    { price: 93, qty: 1M },\n" +
                "    { price: 93, qty: 1M }\n" +
                "  ]\n" +
                "}";
        Object o = Marshallable.fromString(expected);

        Assert.assertEquals(expected, o.toString());
    }
}
