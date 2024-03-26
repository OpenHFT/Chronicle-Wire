/*
 * Copyright 2016-2022 chronicle.software
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
/**
 * An abstract class that serves as a base implementation for the {@link LongConverter} interface.
 * <p>
 * This class delegates the core conversion operations to an encapsulated {@link LongConverter} instance.
 * Subclasses can build upon this base while retaining or customizing the behavior of the underlying converter.
 * </p>
 */
public abstract class AbstractLongConverter implements LongConverter {

    /**
     * Encapsulated instance of {@link LongConverter} that provides core conversion logic.
     */
    protected final LongConverter converter;

    /**
     * Constructs an {@code AbstractLongConverter} using a given set of characters.
     *
     * @param chars set of characters to use for conversion.
     */
    protected AbstractLongConverter(String chars) {
        this(LongConverter.forSymbols(chars));
    }

    /**
     * Constructs an {@code AbstractLongConverter} with a specified converter.
     *
     * @param converter the underlying {@link LongConverter} to be used for conversions.
     */
    protected AbstractLongConverter(LongConverter converter) {
        this.converter = converter;
    }

    /**
     * Retrieves the maximum number of characters that can be parsed by the underlying converter.
     *
     * @return the maximum length for the parsed string.
     */
    @Override
    public int maxParseLength() {
        return converter.maxParseLength();
    }

    /**
     * Parses the provided text using the underlying converter.
     *
     * @param text the text to parse.
     * @return the parsed long value.
     */
    @Override
    public long parse(CharSequence text) {
        return converter.parse(text);
    }

    /**
     * Parses a part of the provided text using the underlying converter.
     *
     * @param text the text to parse.
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return the parsed long value.
     */
    @Override
    public long parse(CharSequence text, int beginIndex, int endIndex) {
        return converter.parse(text, beginIndex, endIndex);
    }

    /**
     * Appends the provided long value to the provided {@code StringBuilder} text.
     *
     * @param text the StringBuilder to append to.
     * @param value the long value to convert and append.
     */
    @Override
    public void append(StringBuilder text, long value) {
        converter.append(text, value);
    }

    /**
     * Appends the provided long value to the provided {@code Bytes<?>} text.
     *
     * @param bytes the Bytes object to append to.
     * @param value the long value to convert and append.
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        converter.append(bytes, value);
    }
}

