//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

/**
 * Implements a Base 85 encoding scheme specifically designed for converting short text strings into long values.
 * <p>
 * The ShortTextLongConverter is optimized for encoding text strings of up to 10 characters. It efficiently handles
 * leading spaces by truncating them, similar to how {@link Base85LongConverter} truncates leading zeros. This feature
 * is particularly useful in scenarios where text padding can be ignored.
 * <p>
 * This class employs a set of 85 characters, resulting in a more compact representation compared to traditional Base64 encoding,
 * especially for larger data sizes. The custom character set includes a mix of punctuation, numbers, uppercase and lowercase letters,
 * and special characters. This extensive character set ensures a wide range of possible encoded values, enhancing the versatility
 * of the encoding process.
 *
 * @see AbstractLongConverter
 * @see Base85LongConverter
 */
public class ShortTextLongConverter extends AbstractLongConverter {

    /**
     * Defines the maximum length of text strings that can be parsed by this converter.
     * The value is set to 10 to optimize for short text strings, making this converter
     * ideal for concise text encoding.
     */
    public static final int MAX_LENGTH = 10;

    /**
     * Provides a readily available instance of ShortTextLongConverter for ease of use.
     * This shared instance allows for convenient access to the converter functionality
     * without the need for repeated instantiations.
     */
    public static final ShortTextLongConverter INSTANCE = new ShortTextLongConverter();

    /**
     * Specifies the custom character set used for the Base85 encoding in this implementation.
     * The character set is thoughtfully chosen to include a diverse range of symbols, letters,
     * and digits, facilitating a robust and flexible encoding process.
     */
    private static final String CHARS = " " +
            "123456789" +
            ":;<=>?@" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
            "abcdefghijklmnopqrstuvwxyz" +
            "\"#$%&'()*+,-./0";

    /**
     * Private constructor to prevent external instantiation.
     * Initializes the converter with the specified custom Base85 character set.
     */
    private ShortTextLongConverter() {
        super(CHARS);
    }

    /**
     * Returns the maximum number of characters that can be parsed.
     *
     * @return the maximum length for the parsed string
     */
    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    /**
     * Specifies that not all characters are safe for the given {@code wireOut}.
     *
     * @return always {@code false}, indicating that caution is required as not all characters are safe for all contexts.
     */
    @Override
    public boolean allSafeChars() {
        return false;
    }
}
