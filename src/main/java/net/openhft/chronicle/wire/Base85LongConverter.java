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

/**
 * Provides a Base85 encoding scheme, converting long values to a string representation.
 * NOTE: This is intended for encoding numbers into text so that small non-negative numbers produce short strings.
 * Notably, leading zero are truncated. In particular, 0 is encoded as an empty string.
 * <p>
 * If you need to encode text as a long value, refer to {@link ShortTextLongConverter}.
 * <p>
 * Base85 is a binary-to-text encoding scheme that represents binary data in an ASCII string format.
 * It uses a set of 85 printable characters to represent the data, which can result in a more compact
 * representation compared to Base64, especially for larger data sizes.
 * <p>
 * This implementation uses a custom character set that includes punctuation, numbers,
 * uppercase letters, lowercase letters, and special characters, ensuring a wider range of
 * encoded values.
 *
 * @see AbstractLongConverter
 * @see ShortTextLongConverter
 */
public class Base85LongConverter extends AbstractLongConverter {

    /**
     * Defines the maximum length of strings that can be parsed by this converter.
     * The value is set to 10, considering the encoding efficiency of Base85.
     */
    public static final int MAX_LENGTH = 10;

    /**
     * Provides a readily available instance of Base85LongConverter for ease of use.
     * This shared instance simplifies the usage pattern by avoiding repeated instantiation.
     */
    public static final Base85LongConverter INSTANCE = new Base85LongConverter();

    /**
     * Specifies the custom character set used for Base85 encoding in this implementation.
     * The character set includes a mix of digits, uppercase and lowercase letters, punctuation,
     * and special symbols, enabling a broad range of encoded outputs.
     */
    private static final String CHARS = "" +
            "0123456789" +
            ":;<=>?@" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
            "abcdefghijklmnopqrstuvwxyz" +
            "\"#$%&'()*+,-./ ";

    /**
     * Private constructor to prevent external instantiation.
     * Initializes the converter with the defined custom Base85 character set.
     */
    private Base85LongConverter() {
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
     * Indicates whether all characters in the custom Base85 character set are safe for the given output context.
     * In this implementation, not all characters are considered safe.
     *
     * @return always {@code false}, denoting that not all characters are safe for all contexts.
     */
    @Override
    public boolean allSafeChars() {
        return false;
    }
}
