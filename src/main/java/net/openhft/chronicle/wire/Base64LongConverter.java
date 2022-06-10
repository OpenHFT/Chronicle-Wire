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
 * Unsigned 64-bit number with encoding with all 0-9, A-Z and a-z, plus period and plus
 */
public class Base64LongConverter implements LongConverter {

    public static final Base64LongConverter INSTANCE = new Base64LongConverter();
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
    private static final PowerOfTwoLongConverter DELEGATE = new PowerOfTwoLongConverter(CHARS);
    public static final int MAX_LENGTH = DELEGATE.maxParseLength();

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
