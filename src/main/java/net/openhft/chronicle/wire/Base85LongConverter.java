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
 * Provides a Base85 encoding scheme for converting long values into a string representation.
 * <p>
 * Base85 is a binary-to-text encoding scheme that represents binary data in an ASCII string format.
 * It uses a set of 85 characters to represent the data, which can result in a more compact
 * representation compared to Base64, especially for larger data sizes.
 * </p>
 *
 * <p>
 * This implementation uses a custom character set that includes punctuation, numbers,
 * uppercase letters, lowercase letters, and special characters, ensuring a wider range of
 * encoded values.
 * </p>
 *
 * @see AbstractLongConverter
 */
public class Base85LongConverter extends AbstractLongConverter {

    /**
     * Maximum length of the parsed string.
     */
    public static final int MAX_LENGTH = 10;

    /**
     * Shared instance of Base85LongConverter for ease of use.
     */
    public static final Base85LongConverter INSTANCE = new Base85LongConverter();

    /**
     * Custom set of characters used for the Base85 encoding.
     */
    private static final String CHARS = "" +
            "0123456789" +
            ":;<=>?@" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
            "abcdefghijklmnopqrstuvwxyz" +
            "\"#$%&'()*+,-./ ";

    /**
     * Private constructor to prevent external instantiation.
     * Initializes the converter with the custom Base85 character set.
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
     * Specifies that not all characters are safe for the given {@code wireOut}.
     *
     * @param wireOut the output for which the safety of characters is checked.
     * @return always returns {@code false} indicating not all characters are safe.
     */
    @Override
    public boolean allSafeChars(WireOut wireOut) {
        return false;
    }
}
