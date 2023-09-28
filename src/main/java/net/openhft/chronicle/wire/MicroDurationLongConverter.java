/*
 * Copyright 2016-2020 chronicle.software
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

import java.time.Duration;

/**
 * Implementation of {@link LongConverter} to convert durations represented as microseconds.
 * This class operates on long values, converting them to and from Java's {@link Duration}.
 */
public class MicroDurationLongConverter implements LongConverter {

    /**
     * Parses the provided {@link CharSequence} into a duration and returns the equivalent duration in microseconds.
     *
     * @param text the {@link CharSequence} to parse
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
     * @param value the duration as a long value in microseconds
     * @return a {@link Duration} representing the same duration
     */
    private Duration duration(long value) {
        return Duration.ofSeconds(value / 1_000_000,
                value % 1_000_000 * 1_000);
    }

    /**
     * Appends a {@link Duration} representation of the provided long value (in microseconds) to the provided {@link StringBuilder}.
     *
     * @param text the {@link StringBuilder} to append to
     * @param value the duration as a long value in microseconds
     */
    @Override
    public void append(StringBuilder text, long value) {
        text.append(duration(value));
    }

    /**
     * Appends a {@link Duration} representation of the provided long value (in microseconds) to the provided {@link Bytes}.
     *
     * @param bytes the {@link Bytes} object to append to
     * @param value the duration as a long value in microseconds
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        bytes.append(duration(value).toString());
    }
}

