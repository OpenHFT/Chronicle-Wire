/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

// Running the test class in a parameterized manner.
class TimestampLongConverterZoneIdsTest extends WireTestCommon {

    private Future<?> future;

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
        assumeFalse(zoneId.equals("GMT0"));
        AbstractTimestampLongConverter mtlc = converterType.createConverter(zoneId);
        final String str = mtlc.asString(converterType.sampleTimeInUTC);
        assertEquals(converterType.sampleTimeInUTC, mtlc.parse(str), zoneId);
    }

    // This test method checks the result of the future from the asynchronous operation.
    @ParameterizedTest
    @MethodSource("combinations")
    void testManyZones(String zoneId, ConverterType converterType, Future<?> future) throws ExecutionException, InterruptedException {
        this.future = future;
        assertNull(future.get());
    }

    // Enum representing the different converter types: Milli, Micro, and Nano.
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

        // The sample time in UTC for each converter type.
        long sampleTimeInUTC;

        ConverterType(long sampleTimeInUTC) {
            this.sampleTimeInUTC = sampleTimeInUTC;
        }
    }

    // Interface that defines a method to create an instance of the timestamp converter based on the zoneId.
    interface ConverterFactory {
        AbstractTimestampLongConverter createConverter(String zoneId);
    }
}
