package net.openhft.chronicle.wire;

import org.junit.Test;

import static net.openhft.chronicle.wire.MicroTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class MicroTimestampLongConverterTest extends WireTestCommon {

    @Test
    public void parse() {
        long now = System.currentTimeMillis();
        // long parse1 = INSTANCE.parse(Long.toString(now));
        // assertEquals(now, parse1 / 1000);
        long parse2 = INSTANCE.parse(Long.toString(now * 1000));
        assertEquals(now, parse2 / 1000);
        long parse3 = INSTANCE.parse(INSTANCE.asString(now * 1000));
        assertEquals(now, parse3 / 1000);
    }

    @Test
    public void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"),
                INSTANCE.parse("2020-09-18T01:02:03.456789"));
    }

    @Test
    public void testTrailingZ() {
        final String text = "2020-09-18T01:02:03.456789";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }

    @Test
    public void NYparse() {
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter("America/New_York");
        long time = INSTANCE.parse("2020/09/18T01:02:03.456789");
        final String str = mtlc.asString(time);
        assertEquals("2020-09-18T01:02:03.456789 America/New_York", str);
        assertEquals(time, mtlc.parse(str));
    }
}