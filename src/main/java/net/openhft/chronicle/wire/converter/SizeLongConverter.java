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
package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.LongConverter;

/**
 * A converter for long values that translates between numeric values and
 * their string representations with a size suffix (K, M, G, T) to indicate
 * the magnitude (kilo, mega, giga, tera). This class implements the
 * {@link LongConverter} interface, providing methods to parse a string
 * with a size suffix back into a long value and to convert a long value
 * into its string representation with the appropriate size suffix.
 */
public class SizeLongConverter implements LongConverter {
    /**
     * The singleton instance of this class.
     */
    public static final SizeLongConverter INSTANCE = new SizeLongConverter();

    /**
     * Parses a string representation of a number with a size suffix
     * (K, M, G, T) and converts it into a long value. The suffix indicates
     * the magnitude (kilo for K, mega for M, giga for G, tera for T) of the
     * number, multiplying the parsed number by 1024 to the power corresponding
     * to the suffix.
     *
     * @param text The text to parse, containing a numeric value followed by
     *             a size suffix (K, M, G, T). Case-insensitive.
     * @return The parsed long value, adjusted for the size suffix. If no
     * suffix is provided, returns the plain numeric value.
     * @throws NumberFormatException If the text before the size suffix is not
     *                               a valid long number or if the text is empty.
     */
    @Override
    public long parse(CharSequence text) throws NumberFormatException {
        int length = text.length();
        if (length < 1)
            throw new NumberFormatException("Empty string");

        int shift;
        switch (text.charAt(length - 1)) {
            case 't':
            case 'T':
                shift = 40;
                break;
            case 'g':
            case 'G':
                shift = 30;
                break;
            case 'm':
            case 'M':
                shift = 20;
                break;
            case 'k':
            case 'K':
                shift = 10;
                break;
            default:
                shift = 0;
                break;
        }
        if (shift != 0) {
            if (length < 2)
                throw new NumberFormatException("No number for prefix '" + text + "'");
            text = text.subSequence(0, length - 1);
        }

        return Long.parseLong(text.toString()) << shift;
    }

    /**
     * Converts a long value to its string representation, appending a size
     * suffix (K, M, G, T) if the value is a multiple of 1024 to a power
     * (kilo, mega, giga, tera, respectively). If the value does not match
     * one of these categories, it is converted to a string without a suffix.
     *
     * @param text  The {@link StringBuilder} to which the converted value
     *              and suffix are appended.
     * @param value The long value to be converted.
     */
    @Override
    public void append(StringBuilder text, long value) {
        if (value == 0)
            text.append('0');
        else if (value >> 40 << 40 == value)
            text.append(value >> 40).append('T');
        else if (value >> 30 << 30 == value)
            text.append(value >> 30).append('G');
        else if (value >> 20 << 20 == value)
            text.append(value >> 20).append('M');
        else if (value >> 10 << 10 == value)
            text.append(value >> 10).append('K');
        else
            text.append(value);
    }
}
