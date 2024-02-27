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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.time.LongTime;
import net.openhft.chronicle.core.time.TimeProvider;

import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;
/**
 * Implementation of {@link LongConverter} for converting timestamps that represent service times.
 * This class allows for system-wide time unit configuration using a system property: 'service.time.unit'.
 * The supported time units are nanoseconds (ns), microseconds (us), and milliseconds (ms). If the system property
 * is not set, it defaults to nanoseconds.
 */
public class ServicesTimestampLongConverter implements LongConverter {
    // The single instance of this class, accessible via reflection.
    @UsedViaReflection
    public static final ServicesTimestampLongConverter INSTANCE = new ServicesTimestampLongConverter();

    private static final String SERVICES_TIME_UNIT = System.getProperty("service.time.unit", "ns");
    private static final longFunction toTime;
    private static final ToLongFunction<TimeProvider> currentTime;
    private static final LongConverter underlying;
    private static final TimeUnit timeUnit;

    static {
        switch (SERVICES_TIME_UNIT) {
            case "ms":
                toTime = LongTime::toMillis;
                currentTime = TimeProvider::currentTimeMillis;
                underlying = MilliTimestampLongConverter.INSTANCE;
                timeUnit = TimeUnit.MILLISECONDS;
                break;
            case "us":
                toTime = LongTime::toMicros;
                currentTime = TimeProvider::currentTimeMicros;
                underlying = MicroTimestampLongConverter.INSTANCE;
                timeUnit = TimeUnit.MICROSECONDS;
                break;
            case "ns":
            default:
                toTime = LongTime::toNanos;
                currentTime = TimeProvider::currentTimeNanos;
                underlying = NanoTimestampLongConverter.INSTANCE;
                timeUnit = TimeUnit.NANOSECONDS;
                break;
        }
    }

    /**
     * Converts the given long value to the configured time unit.
     * @param arg The long value to convert.
     * @return The converted long value.
     */
    public static long toTime(long arg) {
        return toTime.apply(arg);
    }

    public static long currentTime() {
        return currentTime(CLOCK);
    }

    public static long currentTime(TimeProvider clock) {
        return currentTime.applyAsLong(clock);
    }

    public static TimeUnit timeUnit() {
        return timeUnit;
    }

    /**
     * Parses the provided {@link CharSequence} into a timestamp in the configured time unit.
     * @param text The text to parse.
     * @return The parsed timestamp as a long value in the configured time unit.
     */
    @Override
    public long parse(CharSequence text) {
        System.out.printf("DEBUG parse(%s)%n", text);
        return underlying.parse(text);
    }

    /**
     * Parses a part of the provided {@link CharSequence} using the underlying converter.
     *
     * @param text the text to parse.
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return the parsed timestamp as a long value in the configured time unit.
     */
    @Override
    public long parse(CharSequence text, int beginIndex, int endIndex) {
        System.out.printf("DEBUG parse(%s, %s, %s)%n", text, beginIndex, endIndex);
        return underlying.parse(text, beginIndex, endIndex);
    }

    /**
     * Appends a representation of the provided long timestamp (in the configured time unit) to the provided {@link StringBuilder}.
     * @param text The StringBuilder to append to.
     * @param value The timestamp as a long value in the configured time unit.
     */
    @Override
    public void append(StringBuilder text, long value) {
        System.out.printf("DEBUG append(%s, %s)%n", text, value);
        underlying.append(text, value);
    }

    /**
     * Appends a representation of the provided long timestamp (in the configured time unit) to the provided {@link Bytes}.
     * @param bytes The Bytes to append to.
     * @param value The timestamp as a long value in the configured time unit.
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        underlying.append(bytes, value);
    }

    /**
     * Functional interface for long conversion operations.
     */
    interface longFunction {
        long apply(long value);
    }
}
