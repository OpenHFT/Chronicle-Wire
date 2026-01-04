/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;
/**
 * The {@code HexadecimalLongConverter} class is responsible for converting 64-bit unsigned
 * numbers into a hexadecimal representation.
 *
 * <p>This class extends the {@link AbstractLongConverter} class, and inherits its methods
 * to perform the conversions. The class is a singleton, accessed via the {@code INSTANCE}
 * constant. It's thread-safe and can be used across the application.
 *
 * @see AbstractLongConverter
 */
public class HexadecimalLongConverter extends AbstractLongConverter {

    /**
     * Singleton instance for lowercase hexadecimal long conversions.
     */
    public static final LongConverter INSTANCE = new HexadecimalLongConverter();

    // The character set used for hexadecimal encoding
    private static final String CHARS = "0123456789abcdef";

    /**
     * Default constructor that sets up the hexadecimal character set.
     */
    private HexadecimalLongConverter() {
        super(CHARS);
    }
}
