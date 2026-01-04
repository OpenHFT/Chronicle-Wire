/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.StringUtils;

/**
 * A specialized implementation of the {@link net.openhft.chronicle.wire.LongConverter} interface
 * for converting long values to and from strings using power-of-two bases.
 *
 * <p>This converter leverages certain mathematical properties of power-of-two bases
 * to optimize the conversion process.
 */
public class PowerOfTwoLongConverter extends AbstractSymbolsLongConverter {

    // Bit-shift value based on the length of the symbol set.
    private final int shift;

    // Bit-mask for isolating bits.
    private final int mask;

    /**
     * Initialises a converter with a symbol alphabet for power-of-two encoding.
     *
     * @param symbols A string containing unique symbols for conversion. The length of this string
     *                should be a power of 2.
     */
    public PowerOfTwoLongConverter(String symbols) {
        super(symbols);
        final int length = symbols.length();
        assert Maths.isPowerOf2(length); // Ensure length is a power of 2.

        shift = Maths.intLog2(length); // Compute log2 for the length.
        mask = (1 << shift) - 1; // Compute the mask.
    }

    @Override
    protected long accumulate(long value, int symbolIndex) {
        return (value << shift) + symbolIndex;
    }

    /**
     * Appends a long value using the power-of-two alphabet to the supplied builder ({@link StringBuilder}).
     *
     * @param destinationBuilder the StringBuilder to append to
     * @param numericValue the long value to append
     */
    @Override
    public void append(StringBuilder destinationBuilder, long numericValue) {
        int start = destinationBuilder.length();
        while (numericValue != 0) {
            int val = (int) (numericValue & mask); // Isolate bits for the current value.
            destinationBuilder.append(decode[val]);
            numericValue >>>= shift; // Right-shift to move to the next value.
        }

        StringUtils.reverse(destinationBuilder, start); // Reverse the result since it's constructed backward.

        if (destinationBuilder.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "StringBuilder output truncated because value exceeds maximum length");
            destinationBuilder.setLength(start + maxParseLength());
        }
    }

    /**
     * Appends a long value using the power-of-two alphabet to the supplied bytes output ({@link Bytes}).
     *
     * @param destination the Bytes object to append to
     * @param numericValue the long value to append
     */
    @Override
    public void append(Bytes<?> destination, long numericValue) {
        int start = destination.length();
        while (numericValue != 0) {
            int val = (int) (numericValue & mask);
            destination.append(decode[val]);
            numericValue >>>= shift;
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
