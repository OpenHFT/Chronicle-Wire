/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.time.Duration;

/**
 * Implementation of {@link LongConverter} to convert durations represented as microseconds.
 * This class operates on long values, converting them to and from Java's {@link Duration}.
 */
public class MicroDurationLongConverter implements LongConverter {

    /**
     * Parses the provided {@link CharSequence} into a duration and returns the equivalent duration in microseconds.
     *
     * @param text The character sequence representing a duration in ISO-8601
     *             format (e.g., "PT20.345S") to be parsed into total
     *             microseconds.
     * @return the parsed duration as a long value in microseconds
     */
    @Override
    public long parse(CharSequence text) {
        final Duration parse = Duration.parse(text);
        return parse.getSeconds() * 1000_000 + parse.getNano() / 1000;
    }

    /**
     * Converts a duration represented in microseconds to a {@link Duration} object.
     *
     * @param value The duration expressed as a total number of microseconds
     * @return a {@link Duration} representing the same duration
     */
    private Duration duration(long value) {
        return Duration.ofSeconds(value / 1_000_000,
                value % 1_000_000 * 1_000);
    }

    /**
     * Appends a {@link Duration} representation of the provided long value (in microseconds) to the provided {@link StringBuilder}.
     *
     * @param text  The {@link StringBuilder} to which the ISO-8601 string
     *              representation of the duration {@code value} will be
     *              appended.
     * @param value The duration, expressed as a total number of microseconds,
     *              to be formatted and appended
     */
    @Override
    public void append(StringBuilder text, long value) {
        text.append(duration(value));
    }

    /**
     * Appends a {@link Duration} representation of the provided long value (in microseconds) to the provided {@link Bytes}.
     *
     * @param bytes The {@link net.openhft.chronicle.bytes.Bytes} instance to
     *              which the ISO-8601 string representation of the duration
     *              {@code value} will be appended
     * @param value The duration, expressed as a total number of microseconds,
     *              to be formatted and appended
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        bytes.append(duration(value).toString());
    }
}
