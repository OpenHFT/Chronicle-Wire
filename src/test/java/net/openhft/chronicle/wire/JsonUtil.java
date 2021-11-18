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
