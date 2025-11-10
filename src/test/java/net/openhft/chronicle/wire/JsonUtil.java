//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static org.junit.Assert.assertEquals;

// Utility class for JSON-related functions.
public final class JsonUtil {

    // Private constructor to prevent instantiation of this utility class.
    private JsonUtil() {
    }

    // Count the occurrence of a character in a string.
    private static int count(String s, Character c) {
        return (int) s.chars()
                .mapToObj(i -> (char) i)       // Convert intStream to charStream.
                .filter(c::equals)             // Filter the occurrences of the character.
                .count();                      // Count the occurrences.
    }

    // Check the balance of curly and square brackets in a given string.
    public static void assertBalancedBrackets(String input) {
        assertBalancedBrackets(input, '{', '}');
        assertBalancedBrackets(input, '[', ']');
    }

    // Check the balance of specified opening and closing brackets in a given string.
    private static void assertBalancedBrackets(String input,
                                               Character opening,
                                               Character closing) {
        final int openingCount = count(input, opening); // Count of opening brackets.
        final int closingCount = count(input, closing); // Count of closing brackets.

        // Assert equality of counts of opening and closing brackets.
        assertEquals("The number of opening brackets '" + opening + "' is " + openingCount + " but the number of closing brackets '" + closing + "' is " + closingCount,
                openingCount,
                closingCount);

    }
}
