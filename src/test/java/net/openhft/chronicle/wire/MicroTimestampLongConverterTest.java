package net.openhft.chronicle.wire;

import org.junit.Test;

import static net.openhft.chronicle.wire.MicroTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class MicroTimestampLongConverterTest extends WireTestCommon {

    @Test
    public void parse() {
        long now = System.currentTimeMillis();
//        long parse1 = INSTANCE.parse(Long.toString(now));
//        assertEquals(now, parse1 / 1000);
        long parse2 = INSTANCE.parse(Long.toString(now * 1000));
        assertEquals(now, parse2 / 1000);
        long parse3 = INSTANCE.parse(INSTANCE.asString(now * 1000));
        assertEquals(now, parse3 / 1000);
    }
}