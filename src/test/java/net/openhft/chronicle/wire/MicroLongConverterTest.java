package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MicroLongConverterTest extends WireTestCommon {
    @Test
    public void testMicro() {
        String in = "!net.openhft.chronicle.wire.MicroLongConverterTest$Data {\n" +
                "  time: 2019-01-20T23:45:11.123456,\n" +
                "  ttl: PT1H15M\n" +
                "}\n";
        Data data = Marshallable.fromString(in);
        String other_order = "!net.openhft.chronicle.wire.MicroLongConverterTest$Data {\n" +
                "  ttl: PT1H15M,\n" +
                "  time: 2019-01-20T23:45:11.123456\n" +
                "}\n";
        String data_str = data.toString();
        assertTrue(data_str.equals(in) || data_str.equals(other_order));
    }

    static class Data extends SelfDescribingMarshallable {
        @LongConversion(MicroTimestampLongConverter.class)
        long time;
        @LongConversion(MicroDurationLongConverter.class)
        long ttl;
    }
}
