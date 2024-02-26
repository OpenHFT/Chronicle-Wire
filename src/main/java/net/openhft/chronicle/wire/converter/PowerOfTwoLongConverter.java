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
 * A specialized implementation of the {@link LongConverter} interface
 * for converting long values to and from strings using power-of-two bases.
 *
 * <p>This converter leverages certain mathematical properties of power-of-two bases
 * to optimize the conversion process.</p>
 */
public class PowerOfTwoLongConverter implements LongConverter {

    // Bit-shift value based on the length of the symbol set.
    private final int shift;

    // Bit-mask for isolating bits.
    private final int mask;

    // Encoding array for fast look-up.
    private final short[] encode;

    // Decoding array.
    private final char[] decode;

    // Maximum allowed length for parsing.
    private final int maxParseLength;

    /**
     * Initializes a new instance with a given set of symbols.
     *
     * @param symbols A string containing unique symbols for conversion. The length of this string
     *                should be a power of 2.
     */
    public PowerOfTwoLongConverter(String symbols) {
        final int length = symbols.length();
        assert Maths.isPowerOf2(length); // Ensure length is a power of 2.

        shift = Maths.intLog2(length); // Compute log2 for the length.
        mask = (1 << shift) - 1; // Compute the mask.

        decode = symbols.toCharArray();
        encode = new short[128]; // 128 is chosen for ASCII range.
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
     * @param text the character sequence to parse.
     * @return the parsed long value.
     * @throws IllegalArgumentException if the character sequence contains unexpected characters or its length
     *      exceeds the maximum allowable length.
     */
    @Override
    public long parse(CharSequence text) {
        lengthCheck(text);

        return parse0(text, 0, text.length());
    }

    /**
     * Parses a part of a sequence of characters into a long value.
     *
     * @param text the character sequence to parse.
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return the parsed long value.
     * @throws IllegalArgumentException if any of the indices are invalid or the sub-sequence length
     *      exceeds the maximum allowable length.
     */
    @Override
    public long parse(CharSequence text, int beginIndex, int endIndex) {
        lengthCheck(beginIndex, endIndex);

        return parse0(text, beginIndex, endIndex);
    }

    private long parse0(CharSequence text, int beginIndex, int endIndex) {
        long v = 0;
        for (int i = beginIndex; i < endIndex; i++) {
            final char ch = text.charAt(i);

            // Check for characters outside of the encoding range or not present in the encoding map.
            if (ch >= encode.length || encode[ch] < 0)
                throw new IllegalArgumentException("Unexpected character '" + ch + "' in \"" + text + "\"");

            // Convert the character into its corresponding long value.
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
            int val = (int) (value & mask); // Isolate bits for the current value.
            text.append(decode[val]);
            value >>>= shift; // Right-shift to move to the next value.
        }

        StringUtils.reverse(text, start); // Reverse the result since it's constructed backward.

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

        BytesUtil.reverse(text, start); // Reverse the result for bytes.

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.readLimit((long) start + maxParseLength());
        }
    }

    /**
     * Adds an alias character for encoding. The alias character will be treated
     * the same as the "as" character in the encoding process.
     *
     * @param alias The character to treat as an alias.
     * @param as The character that the alias should be treated as.
     */
    public void addEncode(char alias, char as) {
        encode[alias] = encode[as];
    }
}
