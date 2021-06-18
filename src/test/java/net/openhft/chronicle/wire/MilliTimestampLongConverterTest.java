package net.openhft.chronicle.wire;

import org.junit.Test;

import static net.openhft.chronicle.wire.MilliTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class MilliTimestampLongConverterTest extends WireTestCommon {

    @Test
    public void parse() {
        long now = System.currentTimeMillis();
        long parse1 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse1);
        long parse2 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse2);
        String text = INSTANCE.asString(now);
        long parse3 = INSTANCE.parse(text);
        assertEquals(now, parse3);
    }

    @Test
    public void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456"));
    }

    @Test
    public void testTrailingZ() {
        final String text = "2020-09-18T01:02:03.456";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }
}