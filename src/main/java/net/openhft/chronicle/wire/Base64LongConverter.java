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
import net.openhft.chronicle.core.util.StringUtils;

import java.util.Arrays;

public class Base64LongConverter implements LongConverter {

    public static final int MAX_LENGTH = LongConverter.maxParseLength(64);

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    public static final Base64LongConverter INSTANCE = new Base64LongConverter();
    static final char[] CODES = ".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+".toCharArray();
    static final byte[] LOOKUP = new byte[128];

    static {
        Arrays.fill(LOOKUP, (byte) -1);
        for (int i = 0; i < CODES.length; i++) {
            char code = CODES[i];
            LOOKUP[code] = (byte) i;
        }
    }

    @Override
    public long parse(CharSequence text) {
        lengthCheck(text);
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = LOOKUP[text.charAt(i)];
            if (b >= 0)
                v = (v << 6) + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        while (value != 0) {
            text.append(CODES[(int) (value & 0x3F)]);
            value >>>= 6;
        }
        StringUtils.reverse(text, start);

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }
}
