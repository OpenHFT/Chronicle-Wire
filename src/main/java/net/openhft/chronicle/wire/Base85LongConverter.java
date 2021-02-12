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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.StringUtils;

import java.util.Arrays;

public class Base85LongConverter implements LongConverter {

    public static final int MAX_LENGTH = LongConverter.maxParseLength(85);

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    public static final Base85LongConverter INSTANCE = new Base85LongConverter();
    private static final String CHARS =
            "0123456789" +
                    ":;<=>?@" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "\"#$%&'()*+,-./ ";
    private static final char[] DECODE = CHARS.toCharArray();
    private static final byte[] ENCODE = new byte[128];
    private static final int BASE = 85;

    static {
        assert DECODE.length == BASE;
        Arrays.fill(ENCODE, (byte) -1);
        for (int i = 0; i < DECODE.length; i++) {
            char c = DECODE[i];
            ENCODE[c] = (byte) i;
        }
    }


    @Override
    public long parse(CharSequence text) {
        lengthCheck(text);
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = ENCODE[text.charAt(i)];
            if (b >= 0)
                v = v * BASE + b;
        }
        return v;
    }


    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        if (value < 0) {
            long hi = (value >>> 32);
            long h2 = hi / BASE, mod = hi % BASE;
            long val2 = (mod << 32) + (value & 0xFFFFFFFFL);
            int l2 = (int) (val2 / BASE), v = (int) (val2 % BASE);
            text.append(DECODE[v]);
            value = (h2 << 32) + (l2 & 0xFFFFFFFFL);
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append(DECODE[v]);
        }
        StringUtils.reverse(text, start);
        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }


    public void append(Bytes<?> text, long value) {
        final long rp = text.readPosition();
        int start = text.length();
        if (value < 0) {
            long hi = (value >>> 32);
            long h2 = hi / BASE, mod = hi % BASE;
            long val2 = (mod << 32) + (value & 0xFFFFFFFFL);
            int l2 = (int) (val2 / BASE), v = (int) (val2 % BASE);
            text.append(DECODE[v]);
            value = (h2 << 32) + (l2 & 0xFFFFFFFFL);
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append(DECODE[v]);
        }
        BytesUtil.reverse(text, start);
        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.readLimit(rp + start + maxParseLength());
        }
    }


}
