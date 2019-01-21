package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MicroLongConverterTest {
    @Test
    public void testMicro() {
        String in = "!net.openhft.chronicle.wire.MicroLongConverterTest$Data {\n" +
                "  time: 2019-01-20T23:45:11.123456,\n" +
                "  ttl: PT1H15M\n" +
                "}\n";
        Data data = Marshallable.fromString(in);
        assertEquals(in, data.toString());
    }

    static class Data extends AbstractMarshallable {
        @LongConversion(MicroTimestampLongConverter.class)
        long time;
        @LongConversion(MicroDurationLongConverter.class)
        long ttl;
    }
}
