/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;

// Running the test class in a parameterized manner.
@RunWith(value = Parameterized.class)
public class TimestampLongConverterZoneIdsTest extends WireTestCommon {

    // Holds the result of the asynchronous operation.
    private final Future future;

    // The constructor that is invoked for each combination of zoneId and converterType.
    public TimestampLongConverterZoneIdsTest(String zoneId, ConverterType converterType, Future future) {
        this.future = future;
    }

    // This method defines the parameters to be injected into the test class.
    @NotNull
    @Parameterized.Parameters(name = "zoneId={0}, converterType={1}")
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
    static void testManyZones(String zoneId, ConverterType converterType) {
        assumeFalse(zoneId.equals("GMT0"));
        AbstractTimestampLongConverter mtlc = converterType.createConverter(zoneId);
        final String str = mtlc.asString(converterType.sampleTimeInUTC);
        assertEquals(zoneId, converterType.sampleTimeInUTC, mtlc.parse(str));
    }

    // This test method checks the result of the future from the asynchronous operation.
    @Test
    public void testManyZones() throws ExecutionException, InterruptedException {
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
