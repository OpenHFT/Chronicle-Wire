/*
 * Copyright 2016-2025 chronicle.software
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
 */
public abstract class AbstractLongConverter implements LongConverter {

    // Encapsulated instance of LongConverter that provides core conversion functionality.
    protected final LongConverter converter;

    /**
     * Constructs an {@code AbstractLongConverter} using a given set of symbols.
     * Internally, it delegates to {@link LongConverter#forSymbols(String)} to build
     * a converter suited to the supplied symbols.
     *
     * @param symbolSet the characters that define the encoding scheme
     */
    protected AbstractLongConverter(String symbolSet) {
        this(LongConverter.forSymbols(symbolSet));
    }

    /**
     * Constructs an {@code AbstractLongConverter} with a specific converter.
     * This constructor allows subclasses to supply their own implementation.
     *
     * @param underlyingConverter the {@link LongConverter} that performs the work
     */
    protected AbstractLongConverter(LongConverter underlyingConverter) {
        this.converter = underlyingConverter;
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
     * @param textToParse the text to parse
     * @return the parsed long value
     */
    @Override
    public long parse(CharSequence textToParse) {
        return converter.parse(textToParse);
    }

    /**
     * Parses a part of the provided text using the underlying converter.
     *
     * @param textToParse the text to parse
     * @param beginIndex  the beginning index, inclusive
     * @param endIndex    the ending index, exclusive
     * @return the parsed long value.
     */
    @Override
    public long parse(CharSequence textToParse, int beginIndex, int endIndex) {
        return converter.parse(textToParse, beginIndex, endIndex);
    }

    /**
     * Appends the provided long value to the destination {@code StringBuilder}.
     *
     * @param destinationBuilder the builder receiving the formatted value
     * @param numericValue the long value to convert and append
     */
    @Override
    public void append(StringBuilder destinationBuilder, long numericValue) {
        converter.append(destinationBuilder, numericValue);
    }

    /**
     * Appends the provided long value to the destination {@code Bytes}.
     *
     * @param destination the Bytes to append to
     * @param numericValue the long value to convert and append
     */
    @Override
    public void append(Bytes<?> destination, long numericValue) {
        converter.append(destination, numericValue);
    }
}
