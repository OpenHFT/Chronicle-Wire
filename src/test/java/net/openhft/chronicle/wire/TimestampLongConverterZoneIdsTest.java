package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class TimestampLongConverterZoneIdsTest extends WireTestCommon {

    private final Future future;

    public TimestampLongConverterZoneIdsTest(String zoneId, ConverterType converterType, Future future) {
        this.future = future;
    }

    @NotNull
    @Parameterized.Parameters(name = "zoneId={0}, converterType={1}")
    public static Collection<Object[]> combinations() {
        ExecutorService es = ForkJoinPool.commonPool();
        Random random = new Random(-1);
        return ZoneId.getAvailableZoneIds().stream()
                .filter(z -> !z.equals("GMT0"))
                .filter(z -> random.nextInt(10) == 0)
                .flatMap(z -> Arrays.stream(ConverterType.values()).map(ct ->
                        new Object[]{z, ct, es.submit(() -> TimestampLongConverterZoneIdsTest.testManyZones(z, ct))}))
                .collect(Collectors.toList());
    }

    static void testManyZones(String zoneId, ConverterType converterType) {
        assumeFalse(zoneId.equals("GMT0"));
        AbstractTimestampLongConverter mtlc = converterType.createConverter(zoneId);
        final String str = mtlc.asString(converterType.sampleTimeInUTC);
        assertEquals(zoneId, converterType.sampleTimeInUTC, mtlc.parse(str));
    }

    @Test
    public void testManyZones() throws ExecutionException, InterruptedException {
        future.get();
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

    interface ConverterFactory {
        AbstractTimestampLongConverter createConverter(String zoneId);
    }
}