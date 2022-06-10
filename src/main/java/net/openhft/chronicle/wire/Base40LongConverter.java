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

import net.openhft.chronicle.wire.internal.VanillaLongConverter;

/**
 * Unsigned 64-bit number.
 */
@Deprecated(/* to remove in x.25 */)
public class Base40LongConverter implements LongConverter {

    public static final int MAX_LENGTH = LongConverter.maxParseLength(40);
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_^~";
    public static final Base40LongConverter UPPER = new Base40LongConverter(CHARS);
    public static final Base40LongConverter INSTANCE = UPPER;
    public static final Base40LongConverter LOWER = new Base40LongConverter(CHARS.toLowerCase());
    private static final int BASE = 40;
    private final VanillaLongConverter delegate;

    public Base40LongConverter() {
        this(CHARS);
    }

    public Base40LongConverter(String chars) {
        delegate = new VanillaLongConverter(chars);
        // support both cases
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            if (Character.isLowerCase(c))
                delegate.addEncode(Character.toUpperCase(c), c);
            else
                delegate.addEncode(Character.toLowerCase(c), c);
        }
    }

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    @Override
    public long parse(CharSequence text) {
        return delegate.parse(text);
    }

    @Override
    public void append(StringBuilder text, long value) {
        delegate.append(text, value);
    }
}
