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
import net.openhft.chronicle.wire.converter.SymbolsLongConverter;

/**
 * An identifier that acts as a base 66 string of up to 10 characters, or a nanosecond timestamp for dates from 2019-09-14.
 * <p>
 * The base 66 encoding support 0-9, A-Z, a-z, period, underscore, tilde and caret. Leading zeros are truncated.
 * <p>
 * As this is intended for timestamps based on the wall clock, these shouldn't conflict.
 * <p>
 * Negative ids are reserved for application specific encodings.
 */
public class IdentifierLongConverter implements LongConverter {
    public static final IdentifierLongConverter INSTANCE = new IdentifierLongConverter();

    protected static final SymbolsLongConverter SMALL_POSITIVE = new SymbolsLongConverter(
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._~^");
    protected static final long MAX_SMALL_ID = 1568336880910795775L; // 66^10-1
    static final String MIN_DATE = "2019-09-13T01:08:00.910795776";
    static final String MAX_DATE = "2262-04-11T23:47:16.854775807";

    /**
     * Default constructor that's protected to prevent direct instantiation.
     */
    protected IdentifierLongConverter() {
    }

    /**
     * Parses the provided {@link CharSequence} into a long identifier.
     * The parsing behavior changes depending on the length of the CharSequence.
     *
     * @param text the CharSequence to parse
     * @return the parsed long identifier
     */
    @Override
    public long parse(CharSequence text) {
        return text.length() <= 10
                ? SMALL_POSITIVE.parse(text)
                : NanoTimestampLongConverter.INSTANCE.parse(text);
    }

    /**
     * Appends a long identifier to a provided {@link StringBuilder} instance.
     * The behavior changes depending on the magnitude of the identifier.
     *
     * @param text the StringBuilder to append the identifier to
     * @param value the long identifier
     */
    @Override
    public void append(StringBuilder text, long value) {
        if (value < 0)
            throw new IllegalArgumentException("value: " + value); // reserved
        if (value <= MAX_SMALL_ID)
            SMALL_POSITIVE.append(text, value);
        else
            NanoTimestampLongConverter.INSTANCE.append(text, value);
    }

    /**
     * Appends a long identifier to a provided {@link Bytes} instance.
     * The behavior changes depending on the magnitude of the identifier.
     *
     * @param bytes the Bytes to append the identifier to
     * @param value the long identifier
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        if (value < 0)
            throw new IllegalArgumentException("value: " + value); // reserved
        if (value <= MAX_SMALL_ID)
            SMALL_POSITIVE.append(bytes, value);
        else
            NanoTimestampLongConverter.INSTANCE.append(bytes, value);
    }

    /**
     * Returns the maximum length of the {@link CharSequence} that this converter is able
     * to parse, which in this case is the maximum parse length of
     * the {@code NanoTimestampLongConverter}.
     *
     * @return the maximum parse length
     */
    @Override
    public int maxParseLength() {
        return NanoTimestampLongConverter.INSTANCE.maxParseLength();
    }
}
