package net.openhft.chronicle.wire;

import org.junit.Test;

import static net.openhft.chronicle.wire.NanoTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class NanoLongConverterTest extends WireTestCommon {
    @Test
    public void testNano() {
        String in = "!net.openhft.chronicle.wire.NanoLongConverterTest$Data {\n" +
                "  time: 2019-01-20T23:45:11.123456789,\n" +
                "  ttl: PT1H15M\n" +
                "}\n";
        Data data = Marshallable.fromString(in);
        assertEquals(in, data.toString());
    }

    @Test
    public void testTrailingZ() {
        final String text = "2019-01-20T23:45:11.123456789";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }

    static class Data extends SelfDescribingMarshallable {
        @LongConversion(NanoTimestampLongConverter.class)
        long time;
        @LongConversion(NanoDurationLongConverter.class)
        long ttl;
    }
}
