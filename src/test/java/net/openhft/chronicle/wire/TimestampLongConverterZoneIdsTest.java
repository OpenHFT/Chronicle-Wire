/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Running the test class in a parameterized manner.
@SuppressWarnings({"deprecation", "removal"})
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
class TimestampLongConverterZoneIdsTest extends WireTestCommon {

    private Future<?> future;

    void initTimestampLongConverterZoneIdsTest(String zoneId, ConverterType converterType, Future<?> future) {
        this.future = future;
    }

    // This method defines the parameters to be injected into the test class.
    @NotNull
    public static Collection<Object[]> combinations() {
        ExecutorService es = ForkJoinPool.commonPool();
        Random random = new Random(-1);

        // Filter and map all available time zones (excluding GMT0) and converter types.
        return ZoneId.getAvailableZoneIds().stream()
                .filter(z -> !z.equals("GMT0"))
                .filter(z -> random.nextInt(10) == 0)
                .flatMap(z -> Arrays.stream(ConverterType.values()).map(ct ->
                        new Object[]{z, ct, es.submit(() -> TimestampLongConverterZoneIdsTest.testManyZones(z, ct))}))
                .collect(Collectors.toList());
    }

    // This static method tests a given zoneId with the specified converter type.
    private static void testManyZones(String zoneId, ConverterType converterType) {
        assumeFalse(zoneId.equals("GMT0"), "GMT0 zone id is excluded from this test");
        AbstractTimestampLongConverter mtlc = converterType.createConverter(zoneId);
        final String str = mtlc.asString(converterType.sampleTimeInUTC);
        assertEquals(converterType.sampleTimeInUTC, mtlc.parse(str),
                "parsed time should match sample time for zoneId=" + zoneId + ", converterType=" + converterType);
    }

    // This test method checks the result of the future from the asynchronous operation.
    @DisplayName("Converts sample timestamps across zone ids")
    @MethodSource("combinations")
    @ParameterizedTest(name = "zoneId={0}, converterType={1}")
    void testManyZones(String zoneId, ConverterType converterType, Future<?> future) throws ExecutionException, InterruptedException {
        initTimestampLongConverterZoneIdsTest(zoneId, converterType, future);
        assertNull(future.get(), "zone conversion future should return null for zoneId=" + zoneId);
    }

    // Enum representing the different converter types: Milli, Micro, and Nano.
    enum ConverterType implements ConverterFactory {
        Milli(MilliTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123")) {
            @Override
            public MilliTimestampLongConverter createConverter(String zoneId) {
                return new MilliTimestampLongConverter(zoneId);
            }
        },
        Micro(MicroTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456")) {
            @Override
            public MicroTimestampLongConverter createConverter(String zoneId) {
                return new MicroTimestampLongConverter(zoneId);
            }
        },
        Nano(NanoTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456789")) {
            @Override
            public NanoTimestampLongConverter createConverter(String zoneId) {
                return new NanoTimestampLongConverter(zoneId);
            }
        };

        // The sample time in UTC for each converter type.
        final long sampleTimeInUTC;

        ConverterType(long sampleTimeInUTC) {
            this.sampleTimeInUTC = sampleTimeInUTC;
        }
    }

    // Interface that defines a method to create an instance of the timestamp converter based on the zoneId.
    interface ConverterFactory {
        AbstractTimestampLongConverter createConverter(String zoneId);
    }
}
