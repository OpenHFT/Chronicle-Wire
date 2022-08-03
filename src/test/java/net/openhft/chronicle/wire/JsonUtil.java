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

public final class JsonUtil {

    private JsonUtil() {
    }

    static int count(String s, Character c) {
        return (int) s.chars()
                .mapToObj(i -> (char) i)
                .filter(c::equals)
                .count();
    }

    public static void assertBalancedBrackets(String input) {
        assertBalancedBrackets(input, '{', '}');
        assertBalancedBrackets(input, '[', ']');
    }

    static void assertBalancedBrackets(String input,
                                       Character opening,
                                       Character closing) {
        final int openingCount = count(input, opening);
        final int closingCount = count(input, closing);

        assertEquals("The number of opening brackets '" + opening + "' is " + openingCount + " but the number of closing brackets '" + closing + "' is " + closingCount,
                openingCount,
                closingCount);


    }

}
