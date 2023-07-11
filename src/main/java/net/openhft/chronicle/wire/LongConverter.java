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
import net.openhft.chronicle.wire.converter.PowerOfTwoLongConverter;
import net.openhft.chronicle.wire.converter.SymbolsLongConverter;

import static java.lang.Math.log;
import static java.text.MessageFormat.format;

/**
 * This interface provides methods to handle long-to-string conversions and vice versa.
 * It supports conversions with symbols and power of two representations.
 * <p>
 * The converter can be configured to handle special encoding cases with an alias system.
 */
public interface LongConverter {

    /**
     * Creates a {@code LongConverter} instance depending on the provided symbol string.
     * If the length of the symbol string is a power of two, a {@code PowerOfTwoLongConverter} is used.
     * Otherwise, a {@code SymbolsLongConverter} is used.
     *
     * @param chars Symbol string used for conversion.
     * @return an instance of a {@code LongConverter}.
     */
    static LongConverter forSymbols(String chars) {
        return Maths.isPowerOf2(chars.length())
                ? new PowerOfTwoLongConverter(chars)
                : new SymbolsLongConverter(chars);
    }

    /**
     * Calculates the maximum length of a parsed string based on the provided base.
     *
     * @param based The base for the calculation.
     * @return The maximum length of a parsed string.
     */
    static int maxParseLength(int based) {
        return (int) Math.ceil(64 / log(based) * log(2));
    }

    /**
     * Parses the provided {@link CharSequence} and returns the parsed results as a
     * {@code long} primitive.
     *
         * @param text Text to be parsed.
         * @return the parsed {@code text} as an {@code long} primitive.
     */
    long parse(CharSequence text);

    /**
     * Appends the provided {@code value} to the provided {@code text}.
      *
     * @param text Text builder to append the value to.
     * @param value The value to be appended.
         */
    void append(StringBuilder text, long value);

    /**
     * * Appends to provided {@code value} to the provided {@code text}.
     *
     * @param bytes {@code Bytes} object to append the value to.
     * @param value The value to be appended.
     */
    default void append(Bytes<?> bytes, long value) {
        final StringBuilder sb = WireInternal.acquireStringBuilder();
        append(sb, value);
        bytes.append(sb);
    }

    /**
     * Converts the provided long value to a string.
     *
     * @param value The long value to be converted.
     * @return The string representation of the value.
     */
    default String asString(long value) {
        return asText(value).toString();
    }

    /**
     * Converts the provided int value to a {@code CharSequence}.
     *
     * @param value The int value to be converted.
     * @return The {@code CharSequence} representation of the value.
     */
    default CharSequence asText(int value) {
        return asText(value & 0xFFFF_FFFFL);
    }

    /**
     * Converts the provided long value to a {@code CharSequence}.
     *
     * @param value The long value to be converted.
     * @return The {@code CharSequence} representation of the value.
     */
    default CharSequence asText(long value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb;
    }

    /**
     * Returns the maximum number of characters that can be parsed by this converter.
     * By default, it returns Integer.MAX_VALUE which means there is no limit unless overridden.
     *
     * @return the maximum number of characters that can be parsed.
     */
    default int maxParseLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * Checks if the length of the provided text exceeds {@link LongConverter#maxParseLength()}.
     * If it does, an {@code IllegalArgumentException} is thrown.
     *
     * @param text The text to be checked.
     */
    default void lengthCheck(CharSequence text) {
        if (text.length() > maxParseLength())
            throw new IllegalArgumentException(format("text={0} exceeds the maximum allowable length of {1}", text, maxParseLength()));
    }

    /**
     * Checks if all characters are safe to be written to the provided {@code WireOut} without needing escaping or additional quotes for YAML.
     *
     * @param wireOut The output stream to check for safe characters.
     * @return True if no characters need escaping or adding additional quotes for YAML.
     */
    default boolean allSafeChars(WireOut wireOut) {
        return true;
    }

    /**
     * Adds an alias for a character to be encoded as a number.
     * This method is optional and throws an {@code UnsupportedOperationException} if not overridden.
     *
     * @param alias Character to be mapped to a number.
     * @param as    Number to be mapped to the alias character.
     */
    default void addEncode(char alias, char as) {
        throw new UnsupportedOperationException();
    }
}
