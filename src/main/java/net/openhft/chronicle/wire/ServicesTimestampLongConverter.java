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
 * This is the ServicesTimestampLongConverter class implementing the {@link LongConverter} interface.
 * The class is specifically designed to convert timestamps representing service times across different
 * units: nanoseconds (ns), microseconds (us), and milliseconds (ms). The preferred time unit can be
 * specified system-wide using the 'service.time.unit' system property. If not explicitly set, the default
 * unit will be nanoseconds.
 */
public class ServicesTimestampLongConverter implements LongConverter {

    // A singleton instance of this class, primarily for access via reflection.
    @UsedViaReflection
    public static final ServicesTimestampLongConverter INSTANCE = new ServicesTimestampLongConverter();

    // System property to fetch the desired time unit for the service timestamp.
    private static final String SERVICES_TIME_UNIT = System.getProperty("service.time.unit", "ns");

    // Functional interface to convert the time.
    private static final longFunction toTime;

    // Functional interface to fetch the current time.
    private static final ToLongFunction<TimeProvider> currentTime;

    // The underlying LongConverter based on the specified or default time unit.
    private static final LongConverter underlying;

    // The TimeUnit representing the time unit in use.
    private static final TimeUnit timeUnit;

    // Static block to initialize the functions and time unit converters.
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
     *
     * @param arg The long value to convert.
     * @return The converted long value based on the system-configured time unit.
     */
    public static long toTime(long arg) {
        return toTime.apply(arg);
    }

    /**
     * Fetches the current time in the system-configured time unit using the default CLOCK.
     *
     * @return The current time in the system-configured time unit.
     */
    public static long currentTime() {
        return currentTime(CLOCK);
    }

    /**
     * Fetches the current time in the system-configured time unit using the provided TimeProvider.
     *
     * @param clock The TimeProvider to fetch the current time.
     * @return The current time in the system-configured time unit.
     */
    public static long currentTime(TimeProvider clock) {
        return currentTime.applyAsLong(clock);
    }

    /**
     * Returns the current system-configured time unit.
     *
     * @return The time unit as an instance of {@link TimeUnit}.
     */
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
        return underlying.parse(text, beginIndex, endIndex);
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
        return underlying.parse(text, beginIndex, endIndex);
    }

    /**
     * Appends a representation of the provided long timestamp (in the system-configured time unit) to the provided {@link StringBuilder}.
     *
     * @param text  The StringBuilder to which the timestamp representation will be appended.
     * @param value The timestamp as a long value in the system-configured time unit.
     */
    @Override
    public void append(StringBuilder text, long value) {
        underlying.append(text, value);
    }

    /**
     * Appends a representation of the provided long timestamp (in the system-configured time unit) to the provided {@link Bytes}.
     *
     * @param bytes The Bytes object to which the timestamp representation will be appended.
     * @param value The timestamp as a long value in the system-configured time unit.
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        underlying.append(bytes, value);
    }

    /**
     * This is the longFunction interface. It's a functional interface designed to perform operations
     * on long values and return a long result. It is used internally in ServicesTimestampLongConverter
     * to abstract the conversion logic based on the system-configured time unit.
     */
    interface longFunction {
        long apply(long value);
    }
}
