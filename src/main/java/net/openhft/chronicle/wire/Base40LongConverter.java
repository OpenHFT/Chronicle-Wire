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

/**
 * Unsigned 64-bit number.
 */
public class Base40LongConverter implements LongConverter {

    public static final int MAX_LENGTH = LongConverter.maxParseLength(40);

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_:+";
    public static final Base40LongConverter UPPER = new Base40LongConverter(CHARS);
    public static final Base40LongConverter LOWER = new Base40LongConverter(CHARS.toLowerCase());
    public static final Base40LongConverter INSTANCE = UPPER;
    private static final int BASE = 40;
    private final char[] decode;
    private final byte[] encode = new byte[128];

    public Base40LongConverter(String chars) {
        decode = chars.toCharArray();
        assert decode.length == BASE;
        Arrays.fill(encode, (byte) -1);
        // support both cases
        for (int i = 0; i < decode.length; i++) {
            char c = decode[i];
            encode[Character.toLowerCase(c)] = (byte) i;
            encode[Character.toUpperCase(c)] = (byte) i;
        }
    }

    @Override
    public long parse(CharSequence text) {
        lengthCheck(text);
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = encode[text.charAt(i)];
            if (b >= 0)
                v = v * BASE + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        final int start = text.length();
        if (value < 0) {
            long hi = (value >>> 32);
            long h2 = hi / BASE;
            long mod = hi % BASE;
            long val2 = (mod << 32) + (value & 0xFFFFFFFFL);
            int l2 = (int) (val2 / BASE), v = (int) (val2 % BASE);
            text.append(decode[v]);
            value = (h2 << 32) + (l2 & 0xFFFFFFFFL);
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append(decode[v]);
        }
        StringUtils.reverse(text, start);
        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }
}
