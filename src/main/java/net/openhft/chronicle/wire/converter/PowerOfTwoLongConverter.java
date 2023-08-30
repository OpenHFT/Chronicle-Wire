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

package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.wire.LongConverter;

import java.util.Arrays;

/**
 * A converter for long values based on power of two operations.
 * The converter makes use of a specific set of symbols for encoding and decoding operations.
 */
public class PowerOfTwoLongConverter implements LongConverter {
    private final int shift, mask;
    private final short[] encode;
    private final char[] decode;
    private final int maxParseLength;

    /**
     * Initializes a new converter with the given symbols.
     * The number of symbols must be a power of two.
     *
     * @param symbols the symbols to use for encoding and decoding
     */
    public PowerOfTwoLongConverter(String symbols) {
        final int length = symbols.length();
        assert Maths.isPowerOf2(length);
        shift = Maths.intLog2(length);
        mask = (1 << shift) - 1;
        decode = symbols.toCharArray();
        encode = new short[128];
        Arrays.fill(encode, (short) -1);
        for (int i = 0; i < decode.length; i++)
            encode[decode[i]] = (short) i;
        maxParseLength = LongConverter.maxParseLength(length);
    }

    @Override
    public int maxParseLength() {
        return maxParseLength;
    }

    /**
     * Parses a sequence of characters into a long value.
     *
     * @param text the character sequence to parse
     * @return the parsed long value
     * @throws IllegalArgumentException if the character sequence contains unexpected characters
     */
    @Override
    public long parse(CharSequence text) {
        lengthCheck(text);
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            final char ch = text.charAt(i);
            if (ch >= encode.length || encode[ch] < 0)
                throw new IllegalArgumentException("Unexpected character '" + ch + "' in \"" + text + "\"");
            v = (v << shift) + encode[ch];
        }
        return v;
    }

    /**
     * Appends a long value to a StringBuilder.
     *
     * @param text the StringBuilder to append to
     * @param value the long value to append
     */
    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        while (value != 0) {
            int val = (int) (value & mask);
            text.append(decode[val]);
            value >>>= shift;
        }

        StringUtils.reverse(text, start);

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }

    /**
     * Appends a long value to a Bytes object.
     *
     * @param text the Bytes object to append to
     * @param value the long value to append
     */
    @Override
    public void append(Bytes<?> text, long value) {
        int start = text.length();
        while (value != 0) {
            int val = (int) (value & mask);
            text.append(decode[val]);
            value >>>= shift;
        }

        BytesUtil.reverse(text, start);

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.readLimit((long) start + maxParseLength());
        }
    }

    /**
     * Adds an additional alias for a given character in the encoding table.
     *
     * @param alias the character to alias
     * @param as the character to alias it as
     */
    public void addEncode(char alias, char as) {
        encode[alias] = encode[as];
    }
}
