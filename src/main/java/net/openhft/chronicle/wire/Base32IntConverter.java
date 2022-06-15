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

import net.openhft.chronicle.core.Jvm;

/**
 * Unsigned 32-bit number with encoding to be as disambiguated as possible.
 *
 * @deprecated Use Base32LongConverter
 */
@Deprecated(/* to remove in x.25 */)
public class Base32IntConverter implements IntConverter {

    public static final int MAX_LENGTH = IntConverter.maxParseLength(32);
    public static final Base32IntConverter INSTANCE = new Base32IntConverter();

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    @Override
    public int parse(CharSequence text) {
        return (int) Base32LongConverter.INSTANCE.parse(text);
    }

    @Override
    public void append(StringBuilder text, int value) {
        final int start = text.length();
        Base32LongConverter.INSTANCE.append(text, value);
        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }
}
