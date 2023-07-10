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
 * An interface to convert character data to and from different representations.
 * <p>
 * The {@link CharConverter} provides methods to parse a character from a {@link CharSequence},
 * append a character to a {@link StringBuilder}, and convert a character to a string.
 * <p>
 * This can be useful when you want to customize the way your application reads/writes character data.
 */
public interface CharConverter {

    /**
     * Parses the provided {@link CharSequence} and returns the parsed results as a
     * {@code char} primitive.
     *
     * @param text the CharSequence to parse
     * @return the parsed {@code text} as an {@code char} primitive.
     */
    char parse(CharSequence text);

    /**
     * Appends the provided {@code value} to the provided {@code text}.
     *
     * @param text the StringBuilder to which the value is to be appended
     * @param value the character to append to the StringBuilder
     */
    void append(StringBuilder text, char value);

    /**
     * Converts a character value to its String representation.
     * This method uses the {@link #append(StringBuilder, char)} method to append the character to a StringBuilder,
     * and then converts that StringBuilder to a String.
     *
     * @param value the character to convert to a String
     * @return the String representation of the character
     */
    default String asString(final char value) {
        final StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }
}
