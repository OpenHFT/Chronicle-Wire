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

package net.openhft.chronicle.wire;

import static org.junit.Assert.assertEquals;

// Utility class for JSON-related functions.
public final class JsonUtil {

    // Private constructor to prevent instantiation of this utility class.
    private JsonUtil() {
    }

    // Count the occurrence of a character in a string.
    static int count(String s, Character c) {
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
    static void assertBalancedBrackets(String input,
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
