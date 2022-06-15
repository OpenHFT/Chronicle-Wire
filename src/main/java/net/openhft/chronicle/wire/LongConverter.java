/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.wire.internal.PowerOfTwoLongConverter;
import net.openhft.chronicle.wire.internal.VanillaLongConverter;

import static java.lang.Math.log;
import static java.text.MessageFormat.format;

// TODO add a pattern for validation
public interface LongConverter {

    /**
     * Creates an implementation for delegation
     *
     * @param chars symbols to use
     * @return an implementation of a LongConverter
     */
    static LongConverter forSymbols(String chars) {
        return Maths.isPowerOf2(chars.length())
                ? new PowerOfTwoLongConverter(chars)
                : new VanillaLongConverter(chars);
    }

    static int maxParseLength(int based) {
        return (int) Math.ceil(64 / log(based) * log(2));
    }

    /**
     * Parses the provided {@link CharSequence} and returns the parsed results as a
     * {@code long} primitive.
     *
     * @return the parsed {@code text} as an {@code long} primitive.
     */
    long parse(CharSequence text);

    /**
     * Appends the provided {@code value} to the provided {@code text}.
     */
    void append(StringBuilder text, long value);

    /**
     * * Appends to provided {@code value} to the provided {@code text}.
     *
     * @param value to append as text
     * @return bytes to append to
     */
    void append(Bytes<?> bytes, long value);

    default String asString(long value) {
        return asText(value).toString();
    }

    default CharSequence asText(int value) {
        return asText(value & 0xFFFF_FFFFL);
    }

    default CharSequence asText(long value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb;
    }

    /**
     * @return the maximum number of character that this base is able to parse or Integer.MAX_VALUE if this is not enforced
     */
    default int maxParseLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * checks that the length of the text is not greater than {@link LongConverter#maxParseLength()}
     *
     * @param text to check
     */
    default void lengthCheck(CharSequence text) {
        if (text.length() > maxParseLength())
            throw new IllegalArgumentException(format("text={0} exceeds the maximum allowable length of {1}", text, maxParseLength()));
    }

    /**
     * All safe character for a give WireOut type without quotes or escaping.
     *
     * @param wireOut to write to
     * @return true if no characters need escaping or put in additional quotes for YAML
     */
    default boolean allSafeChars(WireOut wireOut) {
        return true;
    }

    /**
     * Add an alias for encoding text as a number
     *
     * @param alias to make the same as another character
     * @param as    to make ti the same as
     */
    default void addEncode(char alias, char as) {
        throw new UnsupportedOperationException();
    }
}
