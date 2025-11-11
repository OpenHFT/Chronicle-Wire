/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * Defines a contract for converting {@code long} values to and from textual
 * representations. Implementations are typically triggered by annotations such
 * as {@link LongConversion} to encode or decode fields using formats like
 * hexadecimal, base64 or symbolic alphabets.
 *
 * <p>Example usage:
 * <pre>{@code
 * LongConverter converter = LongConverter.forSymbols("0123456789ABCDEF");
 * long value = converter.parse("1A");
 * StringBuilder out = new StringBuilder();
 * converter.append(out, value);
 * }</pre>
 *
 * @see LongConversion
 * @see AbstractLongConverter
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
     * Parses the given character sequence, which represents a long value in a
     * specific format, into its numeric form.
     *
     * @param textToParse the character sequence to parse; must not be {@code null}
     * @return the {@code long} value represented by the text
     * @throws IllegalArgumentException if the text cannot be parsed according to
     *         the converter's rules
     */
    long parse(CharSequence textToParse);

    /**
     * Parses a part of the provided {@link CharSequence} and returns the parsed results as a
     * {@code long} primitive.
     * <p>
     * The default implementation is garbage-producing and an implementing class is supposed to reimplement this method.
     *
     * @param textToParse character sequence containing the string representation of the value
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     *
     * @return the parsed {@code textToParse} as a {@code long} value
     * @throws IllegalArgumentException if any of the indices are invalid or the sub-sequence length is
     *      outside of range accepted by a specific converter.
     */
    default long parse(CharSequence textToParse, int beginIndex, int endIndex) {
        return parse(textToParse.toString().substring(beginIndex, endIndex));
    }

    /**
     * Appends the textual form of the provided {@code long} to the given
     * {@link StringBuilder} using the converter's formatting rules.
     *
     * @param destinationBuilder the builder to append to
     * @param numericValue the long value to convert
     */
    void append(StringBuilder destinationBuilder, long numericValue);

    /**
     * Converts the given long value to a string and appends it to the provided Bytes object.
     *
     * @param destination the Bytes object to append to
     * @param numericValue the long value to convert
     */
    default void append(Bytes<?> destination, long numericValue) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            final StringBuilder sb = stlSb.get();
            append(sb, numericValue);
            destination.append(sb);
        }
    }

    /**
     * Converts the given long value to a string.
     *
     * @param numericValue the long value to convert
     * @return The string representation of the value.
     */
    default String asString(long numericValue) {
        return asText(numericValue).toString();
    }

    /**
     * Converts the provided integer value to a CharSequence representation.
     *
     * @param numericValue the integer value to convert
     * @return The CharSequence representation of the value.
     */
    default CharSequence asText(int numericValue) {
        return asText(numericValue & 0xFFFF_FFFFL);
    }

    /**
     * Converts the provided long value to a CharSequence representation.
     *
     * @param numericValue the long value to convert
     * @return The CharSequence representation of the value.
     */
    default CharSequence asText(long numericValue) {
        StringBuilder sb = new StringBuilder();
        append(sb, numericValue);
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
     * @param textToCheck The text to check
     * @throws IllegalArgumentException if the text length exceeds the maximum allowable length.
     */
    default void lengthCheck(CharSequence textToCheck) {
        if (textToCheck.length() > maxParseLength())
            throw new IllegalArgumentException(format("text={0} exceeds the maximum allowable length of {1}", textToCheck, maxParseLength()));
    }

    /**
     * Checks that the length of the provided text does not exceed the allowable maximum.
     *
     * @param textToCheck The text to check
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex   the ending index, exclusive.
     * @throws IllegalArgumentException if the text length exceeds the maximum allowable length.
     */
    default void lengthCheck(CharSequence textToCheck, int beginIndex, int endIndex) {
        if ((beginIndex | endIndex | (endIndex - beginIndex) | (textToCheck.length() - endIndex + beginIndex) | (maxParseLength() - endIndex + beginIndex)) < 0)
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
