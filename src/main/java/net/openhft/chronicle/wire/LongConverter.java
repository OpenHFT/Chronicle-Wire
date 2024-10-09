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
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.wire.converter.PowerOfTwoLongConverter;
import net.openhft.chronicle.wire.converter.SymbolsLongConverter;

import static java.lang.Math.log;
import static java.text.MessageFormat.format;

/**
 * Provides an abstraction for converting between long values and their string representations,
 * potentially based on a custom character or symbol set.
 * <p>
 * The conversion allows encoding long values into compact, human-readable strings and vice versa,
 * useful in contexts where storage efficiency or readability is a concern.
 */
public interface LongConverter {

    /**
     * Creates an instance of the appropriate implementation of
     * based on the length of the provided character set.
     *
     * @param chars A set of symbols or characters to be used in the conversion.
     * @return An instance of . If the length of chars is a power of 2,
     * a {@link PowerOfTwoLongConverter} is returned; otherwise, a {@link SymbolsLongConverter} is returned.
     */
    static LongConverter forSymbols(String chars) {
        return Maths.isPowerOf2(chars.length())
                ? new PowerOfTwoLongConverter(chars)
                : new SymbolsLongConverter(chars);
    }

    /**
     * Calculates the maximum length of a parsed string based on the provided base.
     *
     * @param based The base for conversion.
     * @return The maximum length a string can have to represent a long value.
     */
    static int maxParseLength(int based) {
        return (int) Math.ceil(64 / log(based) * log(2));
    }

    /**
     * Parses the provided {@link CharSequence} and returns the parsed results as a
     * {@code long} primitive.
     *
     * @return the parsed {@code text} as an {@code long} primitive.
     * @throws IllegalArgumentException if the text length is outside of range accepted by a specific converter.
     */
    long parse(CharSequence text);

    /**
     * Parses a part of the provided {@link CharSequence} and returns the parsed results as a
     * {@code long} primitive.
     * <p>
     * The default implementation is garbage-producing and an implementing class is supposed to reimplement this method.
     *
     * @param text character sequence containing the string representation of the value.
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     *
     * @return the parsed {@code text} as an {@code long} primitive.
     * @throws IllegalArgumentException if any of the indices are invalid or the sub-sequence length is
     *      outside of range accepted by a specific converter.
     */
    default long parse(CharSequence text, int beginIndex, int endIndex) {
        return parse(text.toString().substring(beginIndex, endIndex));
    }

    /**
     * Converts the given long value to a string and appends it to the provided StringBuilder.
     *
     * @param text  The StringBuilder to which the converted value is appended.
     * @param value The long value to convert.
     */
    void append(StringBuilder text, long value);

    /**
     * Converts the given long value to a string and appends it to the provided Bytes object.
     *
     * @param bytes The Bytes object to which the converted value is appended.
     * @param value The long value to convert.
     */
    default void append(Bytes<?> bytes, long value) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            final StringBuilder sb = stlSb.get();
            append(sb, value);
            bytes.append(sb);
        }
    }

    /**
     * Converts the given long value to a string.
     *
     * @param value The long value to convert.
     * @return The string representation of the value.
     */
    default String asString(long value) {
        return asText(value).toString();
    }

    /**
     * Converts the provided integer value to a CharSequence representation.
     *
     * @param value The integer value to convert.
     * @return The CharSequence representation of the value.
     */
    default CharSequence asText(int value) {
        return asText(value & 0xFFFF_FFFFL);
    }

    /**
     * Converts the provided long value to a CharSequence representation.
     *
     * @param value The long value to convert.
     * @return The CharSequence representation of the value.
     */
    default CharSequence asText(long value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb;
    }

    /**
     * Returns the maximum number of characters that this converter can parse.
     *
     * @return The maximum parse length, or Integer.MAX_VALUE if there's no limit.
     */
    default int maxParseLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * Checks that the length of the provided text does not exceed the allowable maximum.
     *
     * @param text The text to check.
     * @throws IllegalArgumentException if the text length exceeds the maximum allowable length.
     */
    default void lengthCheck(CharSequence text) {
        if (text.length() > maxParseLength())
            throw new IllegalArgumentException(format("text={0} exceeds the maximum allowable length of {1}", text, maxParseLength()));
    }

    /**
     * Checks that the length of the provided text does not exceed the allowable maximum.
     *
     * @param text The text to check.
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex   the ending index, exclusive.
     * @throws IllegalArgumentException if the text length exceeds the maximum allowable length.
     */
    default void lengthCheck(CharSequence text, int beginIndex, int endIndex) {
        if ((beginIndex | endIndex | (endIndex - beginIndex) | (text.length() - endIndex + beginIndex) | (maxParseLength() - endIndex + beginIndex)) < 0)
            throw new IllegalArgumentException(format("range [{0}, {1}) exceeds the maximum allowable length of {2}",
                    beginIndex, endIndex, maxParseLength()));
    }

    /**
     * Checks if the characters used are "safe",
     * meaning they don't require additional quoting or escaping, especially in contexts
     * like YAML serialization.
     *
     * @return {@code true} if no characters need escaping or additional quoting for JSON or YAML, {@code false} otherwise.
     */
    default boolean allSafeChars() {
        return true;
    }

    /**
     * Introduces a character alias for encoding, facilitating the interpretation of one character
     * as another during encoding.
     *
     * @param alias The character to treat as an alias.
     * @param as    The character that the alias should be treated as.
     * @throws UnsupportedOperationException If the operation is not supported.
     */
    default void addEncode(char alias, char as) {
        throw new UnsupportedOperationException();
    }
}
