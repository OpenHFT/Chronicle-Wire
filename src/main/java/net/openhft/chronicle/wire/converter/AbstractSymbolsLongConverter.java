/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.LongConverter;

import java.util.Arrays;

/**
 * Provides shared parsing behaviour for symbol-based long converters by validating symbols and lengths.
 */
abstract class AbstractSymbolsLongConverter implements LongConverter {
    protected final short[] encode;
    protected final char[] decode;
    private final int maxParseLength;

    protected AbstractSymbolsLongConverter(String symbols) {
        final int length = symbols.length();
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

    @Override
    public long parse(CharSequence textToParse) {
        lengthCheck(textToParse);
        return parse0(textToParse, 0, textToParse.length());
    }

    @Override
    public long parse(CharSequence textToParse, int beginIndex, int endIndex) {
        lengthCheck(textToParse, beginIndex, endIndex);
        return parse0(textToParse, beginIndex, endIndex);
    }

    protected abstract long accumulate(long value, int symbolIndex);

    private long parse0(CharSequence text, int beginIndex, int endIndex) {
        long value = 0;
        for (int i = beginIndex; i < endIndex; i++) {
            final char ch = text.charAt(i);
            if (ch >= encode.length || encode[ch] < 0)
                throw new IllegalArgumentException("Unexpected character '" + ch + "' in \"" + text + "\"");
            value = accumulate(value, encode[ch]);
        }
        return value;
    }
}
