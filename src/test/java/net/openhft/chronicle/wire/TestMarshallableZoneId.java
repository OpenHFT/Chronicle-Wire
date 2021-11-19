package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;

public class TestMarshallableZoneId {

    public static class MySelfDescribingMarshallable extends SelfDescribingMarshallable {
        ZoneId zoneId;
    }

    @Test
    public void testMySelfDescribingMarshallable() {

        final MySelfDescribingMarshallable expected = new MySelfDescribingMarshallable();
        expected.zoneId = ZoneId.of("UTC");

        JSONWire jsonWire = new JSONWire().useTypes(true);
        jsonWire.getValueOut().object(expected);

        final MySelfDescribingMarshallable actual = jsonWire.getValueIn().object(MySelfDescribingMarshallable.class);
        Assert.assertEquals(expected, actual);
    }

    public static class MyAbstractMarshallableCfg extends AbstractMarshallableCfg {
        ZoneId zoneId;
    }

    @Test
    public void testMyAbstractMarshallableCfg() {

        final MyAbstractMarshallableCfg expected = new MyAbstractMarshallableCfg();
        expected.zoneId = ZoneId.of("UTC");

        JSONWire jsonWire = new JSONWire().useTypes(true);
        jsonWire.getValueOut().object(expected);

        final MyAbstractMarshallableCfg actual = jsonWire.getValueIn().object(MyAbstractMarshallableCfg.class);
        Assert.assertEquals(expected, actual);
    }

}
