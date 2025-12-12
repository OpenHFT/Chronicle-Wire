/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.time.Duration;

/**
 * Implementation of {@link LongConverter} for converting durations represented as nanoseconds.
 * This class operates on long values, converting them to and from Java's {@link Duration}.
 */
public class NanoDurationLongConverter implements LongConverter {

    /**
     * Creates a converter for durations expressed in nanoseconds.
     */
    public NanoDurationLongConverter() {
    }

    /**
     * Parse a duration in ISO-8601 format and return the equivalent value in nanoseconds.
     *
     * @param text The character sequence representing a duration in ISO-8601 format (e.g., "PT20.345S") to be parsed into total nanoseconds.
     * @return the parsed duration as a long value in nanoseconds
     */
    @Override
    public long parse(CharSequence text) {
        final Duration parse = Duration.parse(text);
        return parse.getSeconds() * 1_000_000_000L + parse.getNano();
    }

    /**
     * Convert a number of nanoseconds to a {@link Duration}.
     *
     * @param value The duration expressed as a total number of nanoseconds.
     * @return a {@link Duration} representing the same duration
     */
    private Duration duration(long value) {
        return Duration.ofSeconds(value / 1_000_000_000L,
                value % 1_000_000_000L);
    }

    /**
     * Append the ISO-8601 form of the given duration to the supplied {@link StringBuilder}.
     *
     * @param text The {@link StringBuilder} to which the ISO-8601 string representation of the duration {@code value} will be appended.
     * @param value The duration, expressed as a total number of nanoseconds, to be formatted and appended.
     */
    @Override
    public void append(StringBuilder text, long value) {
        text.append(duration(value));
    }

    /**
     * Append the ISO-8601 form of the given duration to the supplied {@link Bytes}.
     *
     * @param bytes The {@link net.openhft.chronicle.bytes.Bytes} instance to which the ISO-8601 string representation of the duration {@code value} will be appended.
     * @param value The duration, expressed as a total number of nanoseconds, to be formatted and appended.
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        bytes.append(duration(value).toString());
    }
}
