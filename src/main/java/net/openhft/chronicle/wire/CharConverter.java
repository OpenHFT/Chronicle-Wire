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
 * Represents a converter for transforming characters to and from textual representations.
 * This interface provides methods for parsing a {@link CharSequence} to produce a character and
 * for appending a character to a {@link StringBuilder}.
 * <p>
 * Implementations can use this interface to create custom representations or transformations
 * for characters as needed.
 *
 * @since 2023-09-14
 */
public interface CharConverter {

    /**
     * Parses the provided {@link CharSequence} to produce a character. Implementing classes
     * should define how the text is transformed to a character primitive.
     *
     * @param text the textual representation of a character to be parsed.
     * @return the parsed character.
     * @throws IllegalArgumentException if the provided {@code text} cannot be parsed into a char.
     */
    char parse(CharSequence text);

    /**
     * Appends the string representation of the provided character {@code value} to the end
     * of the {@code text}. Implementing classes should define how the character is transformed
     * to its textual representation.
     *
     * @param text  the {@link StringBuilder} to which the character's textual representation should be appended.
     * @param value the character to be converted and appended to the text.
     */
    void append(StringBuilder text, char value);

    /**
     * Converts the provided {@code value} into its string representation.
     *
     * @param value the character to be converted to its string representation.
     * @return the string representation of the provided character.
     */
    default String asString(final char value) {
        final StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }
}
