//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * This class is responsible for converting 64-bit unsigned numbers into a base32 representation.
 * The base32 representation uses a specific character set defined by RFC 4648 (though not the same order) to ensure readability and to avoid common interpretation mistakes.
 * For example, it avoids using similar characters such as '0' and 'O', or '1' and 'l'.
 * <p>
 * This class extends the {@code AbstractLongConverter} class and reuses its methods to perform the conversions.
 *
 * <p>The class is a singleton, accessed via the {@code INSTANCE} constant. It's thread-safe and can be used across the application.
 *
 * @see AbstractLongConverter
 */
public class Base32LongConverter extends AbstractLongConverter {

    /**
     * The singleton instance of {@code Base32LongConverter}.
     */
    public static final Base32LongConverter INSTANCE = new Base32LongConverter();

    // The character set used for base32 encoding
    private static final String CHARS = "234567ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Default constructor that sets up base32 character set and configures aliases to avoid common character misinterpretations.
     */
    public Base32LongConverter() {
        super(CHARS);

        // Avoid confusion between similar looking characters
        converter.addEncode('0', 'O');
        converter.addEncode('1', 'l');
        converter.addEncode('8', 'B');
        converter.addEncode('9', 'q');

        // Encode lowercase characters as their uppercase counterparts
        for (char ch = 'a'; ch <= 'z'; ch++)
            converter.addEncode(ch, Character.toUpperCase(ch));
    }
}
