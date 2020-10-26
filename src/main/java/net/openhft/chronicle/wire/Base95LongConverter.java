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

import net.openhft.chronicle.core.util.StringUtils;

import java.math.BigInteger;

public class Base95LongConverter implements LongConverter {
    public static final Base95LongConverter INSTANCE = new Base95LongConverter();
    private static final int BASE = 95;
    private static final BigInteger BASE_BI = BigInteger.valueOf(BASE);
    private static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(64);

    @Override
    public long parse(CharSequence text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++)
            v = v * BASE + text.charAt(i) - ' ' + 1;
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        if (value < 0) {
            BigInteger bi = BigInteger.valueOf(value).add(TWO_TO_64);
            int v = bi.mod(BASE_BI).intValueExact();
            value = bi.divide(BASE_BI).longValueExact();
            text.append((char) (' ' + v - 1));
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append((char) (' ' + v - 1));
        }
        StringUtils.reverse(text, start);
    }
}
