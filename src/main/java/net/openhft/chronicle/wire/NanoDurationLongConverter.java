/*
 * Copyright 2016-2025 chronicle.software
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

import java.time.Duration;

/**
 * Implementation of {@link LongConverter} for converting durations represented as nanoseconds.
 * This class operates on long values, converting them to and from Java's {@link Duration}.
 */
public class NanoDurationLongConverter implements LongConverter {

    /**
     * Parses the provided {@link CharSequence} into a duration and returns the equivalent duration in nanoseconds.
     *
     * @param textToParse the {@link CharSequence} to parse
     * @return the parsed duration as a long value in nanoseconds
     */
    @Override
    public long parse(CharSequence textToParse) {
        final Duration parse = Duration.parse(textToParse);
        return parse.getSeconds() * 1_000_000_000L + parse.getNano();
    }

    /**
     * Converts a duration represented in nanoseconds to a {@link Duration} object.
     *
     * @param value the duration as a long value in nanoseconds
     * @return a {@link Duration} representing the same duration
     */
    private Duration duration(long value) {
        return Duration.ofSeconds(value / 1_000_000_000L,
                value % 1_000_000_000L);
    }

    /**
     * Appends a {@link Duration} representation of the provided long value (in nanoseconds) to the provided {@link StringBuilder}.
     *
     * @param outputBuilder the {@link StringBuilder} to append to
     * @param numericValue the duration as a long value in nanoseconds
     */
    @Override
    public void append(StringBuilder outputBuilder, long numericValue) {
        outputBuilder.append(duration(numericValue));
    }

    /**
     * Appends a {@link Duration} representation of the provided long value (in nanoseconds) to the provided {@link Bytes}.
     *
     * @param outputBytes the {@link Bytes} object to append to
     * @param numericValue the duration as a long value in nanoseconds
     */
    @Override
    public void append(Bytes<?> outputBytes, long numericValue) {
        outputBytes.append(duration(numericValue).toString());
    }
}
