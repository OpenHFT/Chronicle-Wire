package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class TimestampLongConverterZoneIdsTest extends WireTestCommon {
    final ConverterType converterType;
    final String zoneId;

    interface ConverterFactory {
        AbstractTimestampLongConverter createConverter(String zoneId);
    }

    enum ConverterType implements ConverterFactory {
        Milli(MilliTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123")) {
            public MilliTimestampLongConverter createConverter(String zoneId) {
                return new MilliTimestampLongConverter(zoneId);
            }
        },
        Micro(MicroTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456")) {
            public MicroTimestampLongConverter createConverter(String zoneId) {
                return new MicroTimestampLongConverter(zoneId);
            }
        },
        Nano(NanoTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456789")) {
            public NanoTimestampLongConverter createConverter(String zoneId) {
                return new NanoTimestampLongConverter(zoneId);
            }
        };
        long sampleTimeInUTC;

        ConverterType(long sampleTimeInUTC) {
            this.sampleTimeInUTC = sampleTimeInUTC;
        }
    }

    public TimestampLongConverterZoneIdsTest(String zoneId, ConverterType converterType) {
        this.zoneId = zoneId;
        this.converterType = converterType;
    }

    @NotNull
    @Parameterized.Parameters(name = "zoneId={0}, converterType={1}")
    public static Collection<Object[]> combinations() {
        return ZoneId.getAvailableZoneIds().stream()
                .flatMap(z -> Arrays.stream(ConverterType.values()).map(ct -> new Object[]{z, ct}))
                .parallel()
                .filter(s -> ThreadLocalRandom.current().nextInt(10) == 0)
                .collect(Collectors.toList());
    }

    @Test
    public void testManyZones() {
        assumeFalse(zoneId.equals("GMT0"));
        AbstractTimestampLongConverter mtlc = converterType.createConverter(zoneId);
        final String str = mtlc.asString(converterType.sampleTimeInUTC);
        assertEquals(zoneId, converterType.sampleTimeInUTC, mtlc.parse(str));
    }
}