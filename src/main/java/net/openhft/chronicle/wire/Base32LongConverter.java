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

import net.openhft.chronicle.wire.internal.PowerOfTwoLongConverter;

/**
 * Unsigned 64-bit number with encoding to be as disambiguated as possible.
 */
public class Base32LongConverter implements LongConverter {

    public static final Base32LongConverter INSTANCE = new Base32LongConverter();
    private static final String CHARS = "234567ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final PowerOfTwoLongConverter DELEGATE = new PowerOfTwoLongConverter(CHARS);
    public static final int MAX_LENGTH = DELEGATE.maxParseLength();

    static {
        DELEGATE.addEncode('0', 'O');
        DELEGATE.addEncode('1', 'l');
        DELEGATE.addEncode('8', 'B');
        DELEGATE.addEncode('9', 'q');
        for (char ch = 'a'; ch <= 'z'; ch++)
            DELEGATE.addEncode(ch, Character.toUpperCase(ch));
    }

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    @Override
    public long parse(CharSequence text) {
        return DELEGATE.parse(text);
    }

    @Override
    public void append(StringBuilder text, long value) {
        DELEGATE.append(text, value);
    }
}
