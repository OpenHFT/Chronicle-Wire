/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
     * Converts a timestamp to the service's configured time unit.
     *
     * @param arg The timestamp value, typically nanoseconds since epoch (from
     *            {@link System#nanoTime()} or
     *            {@link net.openhft.chronicle.core.time.TimeProvider#currentTimeNanos()}),
     *            to be converted to the service time unit.
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
     * @param clock The {@link net.openhft.chronicle.core.time.TimeProvider} instance used to obtain
     *              the current time. This allows for custom time sources, for example in tests.
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
     * @param text The character sequence representing a date-time string, which will be parsed by
     *             the underlying timestamp converter (for example
     *             {@link NanoTimestampLongConverter} or {@link MicroTimestampLongConverter}) based
     *             on the service time unit configuration.
     * @return The parsed timestamp as a long value in the configured time unit.
     */
    @Override
    public long parse(CharSequence text) {
        return underlying.parse(text);
    }

    /**
     * Parses a part of the provided {@link CharSequence} using the underlying converter.
     *
     * @param text The character sequence containing the date-time string to parse.
     * @param beginIndex The starting index (inclusive) of the subsequence in {@code text} to be parsed.
     * @param endIndex The ending index (exclusive) of the subsequence in {@code text} to be parsed.
     * @return the parsed timestamp as a long value in the configured time unit.
     */
    @Override
    public long parse(CharSequence text, int beginIndex, int endIndex) {
        return underlying.parse(text, beginIndex, endIndex);
    }

    /**
     * Appends a representation of the provided long timestamp (in the system-configured time unit) to the provided {@link StringBuilder}.
     *
     * @param text  The {@link StringBuilder} to which the formatted string representation of the
     *              timestamp {@code value} will be appended, using the service's configured time
     *              unit and associated formatter.
     * @param value The timestamp value, in the service's configured time unit, to be formatted and
     *              appended.
     */
    @Override
    public void append(StringBuilder text, long value) {
        underlying.append(text, value);
    }

    /**
     * Appends a representation of the provided long timestamp (in the system-configured time unit) to the provided {@link Bytes}.
     *
     * @param bytes The {@link net.openhft.chronicle.bytes.Bytes} instance to which the formatted
     *              string representation of the timestamp {@code value} will be appended, using the
     *              service's configured time unit and associated formatter.
     * @param value The timestamp value, in the service's configured time unit, to be formatted and
     *              appended.
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
        /**
         * Processes a long value and returns the result.
         *
         * @param value The input long value to be processed by the function.
         * @return The processed value.
         */
        long apply(long value);
    }
}
