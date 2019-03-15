package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base40IntConverterTest {
    @Test
    public void parse() {
        Base40IntConverter bic = new Base40IntConverter();
        for (String s : ",A,0,ZZ,99,ABCDEF,012345,ZZZZZZ,999999".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)));
        }
        for (int l : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            assertEquals(l, bic.parse(bic.asString(l)));
        }
    }
}