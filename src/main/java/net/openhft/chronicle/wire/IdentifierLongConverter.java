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
 * This is the IdentifierLongConverter class implementing the LongConverter interface.
 * It facilitates the transformation between base 66 encoded strings and their corresponding long representations.
 * Specifically, the class can handle either short base 66 encoded strings (of up to 10 characters) or nanosecond timestamps
 * within a specified date range. Note that negative IDs are reserved for application-specific encodings.
 *
 * @since 2023-09-12
 */
public class IdentifierLongConverter implements LongConverter {

    // Singleton instance for the IdentifierLongConverter
    public static final IdentifierLongConverter INSTANCE = new IdentifierLongConverter();

    // Converter for short base 66 encoded strings
    protected static final SymbolsLongConverter SMALL_POSITIVE = new SymbolsLongConverter(
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._~^");
    // Maximum possible id for base 66 encoded strings of length up to 10
    protected static final long MAX_SMALL_ID = 1568336880910795775L;

    // The specified start and end timestamps for which the converter can operate
    static final String MIN_DATE = "2019-09-13T01:08:00.910795776";
    static final String MAX_DATE = "2262-04-11T23:47:16.854775807";

    /**
     * Default constructor, which is protected to ensure that the class follows a singleton pattern.
     * This prevents unnecessary multiple instances of the converter.
     */
    protected IdentifierLongConverter() {
    }

    /**
     * Parses a given CharSequence into a long identifier. The conversion behavior varies based on the
     * length of the input text.
     * <p>
     * If the text length is less than or equal to 10, it is treated as a base 66 encoded string. Otherwise,
     * it is treated as a nanosecond timestamp.
     * </p>
     * @param text The CharSequence to be parsed.
     * @return The parsed long identifier representation.
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
