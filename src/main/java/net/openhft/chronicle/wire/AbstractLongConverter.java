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

public abstract class AbstractLongConverter implements LongConverter {
    protected final LongConverter converter;

    public AbstractLongConverter(String chars) {
        this(LongConverter.forSymbols(chars));
    }

    public AbstractLongConverter(LongConverter converter) {
        this.converter = converter;
    }

    @Override
    public int maxParseLength() {
        return converter.maxParseLength();
    }

    @Override
    public long parse(CharSequence text) {
        return converter.parse(text);
    }

    @Override
    public void append(StringBuilder text, long value) {
        converter.append(text, value);
    }

    @Override
    public void append(Bytes<?> bytes, long value) {
        converter.append(bytes, value);
    }
}
