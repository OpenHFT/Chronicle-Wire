/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.StringUtils;

/**
 * A specialized implementation of the {@link net.openhft.chronicle.wire.LongConverter} interface for
 * converting long values to and from strings using arbitrary bases, specifically
 * those not necessarily in powers of two.
 *
 * <p>This converter efficiently manages conversion using provided symbols,
 * allowing flexible and adaptable encoding and decoding processes.
 */
public class SymbolsLongConverter extends AbstractSymbolsLongConverter {

    // Multiplicative factor for the conversion based on symbol length.
    private final int factor;

    /**
     * Initialises a converter with a custom symbol alphabet for base conversion.
     *
     * @param symbols A string containing unique symbols for conversion.
     */
    public SymbolsLongConverter(String symbols) {
        super(symbols);
        factor = symbols.length();
    }

    @Override
    protected long accumulate(long value, int symbolIndex) {
        return value * factor + symbolIndex;
    }

    /**
     * Appends a long value using the symbol alphabet to the supplied builder ({@link StringBuilder}).
     *
     * @param destinationBuilder the StringBuilder to append to
     * @param numericValue the long value to append
     */
    @Override
    public void append(StringBuilder destinationBuilder, long numericValue) {
        final int start = destinationBuilder.length();

        // Handle negative values by converting them using unsigned operations.
        if (numericValue < 0) {
            int v = (int) Long.remainderUnsigned(numericValue, factor);
            numericValue = Long.divideUnsigned(numericValue, factor);
            destinationBuilder.append(decode[v]);
        }

        while (numericValue != 0) {
            int v = (int) (numericValue % factor);
            numericValue /= factor;
            destinationBuilder.append(decode[v]);
        }

        StringUtils.reverse(destinationBuilder, start); // Reverse the result since it's constructed backward.

        if (destinationBuilder.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "StringBuilder output truncated because value exceeds maximum length");
            destinationBuilder.setLength(start + maxParseLength());
        }
    }

    /**
     * Appends a long value using the symbol alphabet to the supplied bytes output ({@link Bytes}).
     *
     * @param destination the Bytes object to append to
     * @param numericValue the long value to append
     */
    @Override
    public void append(Bytes<?> destination, long numericValue) {
        final int start = destination.length();

        // Handle negative values in bytes format.
        if (numericValue < 0) {
            int v = (int) Long.remainderUnsigned(numericValue, factor);
            numericValue = Long.divideUnsigned(numericValue, factor);
            destination.append(decode[v]);
        }

        while (numericValue != 0) {
            int v = (int) (numericValue % factor);
            numericValue /= factor;
            destination.append(decode[v]);
        }

        BytesUtil.reverse(destination, start); // Reverse the result for bytes.

        if (destination.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "Bytes output truncated because value exceeds maximum length");
            destination.readLimit((long) start + maxParseLength());
        }
    }

    /**
     * Adds an alias character for encoding. The alias character will be treated
     * the same as the "as" character in the encoding process.
     *
     * @param alias The character to treat as an alias.
     * @param as The character that the alias should be treated as.
     */
    @Override
    public void addEncode(char alias, char as) {
        encode[alias] = encode[as];
    }
}
