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
 * Unsigned 32-bit number.
 */
public class Base40IntConverter implements IntConverter {
    public static final int MAX_LENGTH = IntConverter.maxParseLength(40);
    public static final Base40IntConverter INSTANCE = new Base40IntConverter();
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:-+";
    private static final char[] DECODE = CHARS.toCharArray();
    private static final byte[] ENCODE = new byte[128];

    private static final int BASE = 40;

    static {
        assert DECODE.length == BASE;
        Arrays.fill(ENCODE, (byte) -1);
        for (int i = 0; i < DECODE.length; i++) {
            char c = DECODE[i];
            ENCODE[c] = (byte) i;
        }
    }

    @Override
    public int parse(CharSequence text) {
        lengthCheck(text);
        int v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = ENCODE[text.charAt(i)];
            if (b >= 0)
                v = v * BASE + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, int value) {
        int start = text.length();
        long value2 = value & 0xFFFFFFFFL;
        while (value2 != 0) {
            int v = (int) (value2 % BASE);
            value2 /= BASE;
            text.append(DECODE[v]);
        }
        StringUtils.reverse(text, start);
        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }
}
