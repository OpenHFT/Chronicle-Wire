/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import static java.lang.Math.log;
import static java.text.MessageFormat.format;

// TODO add a pattern for validation
@Deprecated(/* use LongConversion, to be removed in x.25 */)
public interface IntConverter {

    /**
     * Parses the provided {@link CharSequence} and returns the parsed results as an
     * {@code int} primitive.
     *
     * @return the parsed {@code text} as an {@code int} primitive.
     */
    int parse(CharSequence text);

    /**
     * Appends the provided {@code value} to the provided {@code text}.
     */
    void append(StringBuilder text, int value);

    default String asString(final int value) {
        final StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }

    default CharSequence asText(final int value) {
        final StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb;
    }

    /**
     * @return the maximum number of character that this base is able to parse or Integer.MAX_VALUE if this is not enforced
     */
    default int maxParseLength() {
        return Integer.MAX_VALUE;
    }

    static int maxParseLength(int based) {
        return (int) Math.ceil(32 / log(based) * log(2));
    }

    /**
     * checks that the length of the text is not greater than {@link LongConverter#maxParseLength()}
     *
     * @param text to check
     */
    default void lengthCheck(CharSequence text) {
        if (text.length() > maxParseLength())
            throw new IllegalArgumentException(format("text={0} exceeds the maximum allowable length of {1}", text, maxParseLength()));
    }
}