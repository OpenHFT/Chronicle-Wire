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
 * The DelegatingLongConverter class is an abstract class that implements the LongConverter interface.
 * It delegates all the method calls to a given LongConverter instance.
 */
public abstract class DelegatingLongConverter implements LongConverter {
    protected final LongConverter converter;

    /**
     * Constructs a DelegatingLongConverter with a given set of characters.
     * It internally uses the {@link LongConverter#forSymbols(String)} method to create the internal converter.
     *
     * @param chars the characters to be used for the internal converter
     */
    protected DelegatingLongConverter(String chars) {
        this(LongConverter.forSymbols(chars));
    }

    /**
     * Constructs a DelegatingLongConverter with a given LongConverter instance.
     * All method calls will be delegated to this converter.
     *
     * @param converter the LongConverter instance to be used for delegation
     */
    protected DelegatingLongConverter(LongConverter converter) {
        this.converter = converter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int maxParseLength() {
        return converter.maxParseLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long parse(CharSequence text) {
        return converter.parse(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void append(StringBuilder text, long value) {
        converter.append(text, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        converter.append(bytes, value);
    }
}

