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
 * The LongConverter interface provides methods for parsing long primitives
 * from character sequences and for appending long primitives to StringBuilder and Bytes.
 * Also, the interface provides an ability to manage parsing length limits and encoding text as a number.
 * This interface has two concrete implementations: {@link PowerOfTwoLongConverter} and {@link SymbolsLongConverter}.
 */
public interface LongConverter {

    /**
     * Creates a LongConverter implementation according to the given symbols.
     *
     * @param chars symbols to use
     * @return a {@link PowerOfTwoLongConverter} implementation if the length of chars is power of 2, otherwise a {@link SymbolsLongConverter}
     */
    static LongConverter forSymbols(String chars) {
        return Maths.isPowerOf2(chars.length())
                ? new PowerOfTwoLongConverter(chars)
                : new SymbolsLongConverter(chars);
    }

    /**
     * Calculates the maximum number of characters that can be parsed, based on the given radix.
     * This method uses the formula: ceil(64 / log(based) * log(2)).
     *
     * @param based the radix to use for the calculation
     * @return the maximum number of characters that can be parsed
     */
    static int maxParseLength(int based) {
        return (int) Math.ceil(64 / log(based) * log(2));
    }

    /**
     * Parses the provided text as a long primitive.
     *
     * @param text the CharSequence to be parsed
     * @return the parsed text as a long primitive
     */
    long parse(CharSequence text);

    /**
     * Appends the provided value to the given StringBuilder text.
     *
     * @param text StringBuilder to which the value will be appended
     * @param value the long value to be appended
     */
    void append(StringBuilder text, long value);

    /**
     * Appends the provided value to the given Bytes.
     *
     * @param bytes Bytes to which the value will be appended
     * @param value the long value to be appended
     */
    default void append(Bytes<?> bytes, long value) {
        final StringBuilder sb = WireInternal.acquireStringBuilder();
        append(sb, value);
        bytes.append(sb);
    }

    /**
     * Converts the provided long value to a String.
     *
     * @param value the long value to be converted
     * @return the String representation of the value
     */
    default String asString(long value) {
        return asText(value).toString();
    }

    /**
     * Converts the provided int value to a CharSequence.
     *
     * @param value the int value to be converted
     * @return the CharSequence representation of the value
     */
    default CharSequence asText(int value) {
        return asText(value & 0xFFFF_FFFFL);
    }

    /**
     * Converts the provided long value to a CharSequence.
     *
     * @param value the long value to be converted
     * @return the CharSequence representation of the value
     */
    default CharSequence asText(long value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb;
    }

    /**
     * Returns the maximum number of characters that this base is able to parse or Integer.MAX_VALUE if this is not enforced.
     *
     * @return the maximum allowable parsing length
     */
    default int maxParseLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * Verifies that the length of the provided text is not greater than the maximum parse length.
     *
     * @param text the CharSequence to check
     * @throws IllegalArgumentException if text length exceeds the maximum allowable length
     */
    default void lengthCheck(CharSequence text) {
        if (text.length() > maxParseLength())
            throw new IllegalArgumentException(format("text={0} exceeds the maximum allowable length of {1}", text, maxParseLength()));
    }

    /**
     * Checks if all characters of a given WireOut type are safe without quotes or escaping.
     *
     * @param wireOut the WireOut instance to write to
     * @return true if no characters need escaping or put in additional quotes for YAML
     */
    default boolean allSafeChars(WireOut wireOut) {
        return true;
    }

    /**
     * Adds an alias for encoding text as a number.
     *
     * @param alias character to be made the same as another character
     * @param as character to be used as the alias
     * @throws UnsupportedOperationException if this method is not supported
     */
    default void addEncode(char alias, char as) {
        throw new UnsupportedOperationException();
    }
}
