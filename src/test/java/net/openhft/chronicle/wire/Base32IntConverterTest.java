package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base32IntConverterTest {
    @Test
    public void parse() {
        IntConverter bic = new Base32IntConverter();
        for (String s : ",O,A,L,ZZ,QQ,ABCDEF,OL2345,ZZZZZZ,QQQQQQ,5ZZZZZZ".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)));
            assertEquals(s, bic.asString(bic.parse(s.toLowerCase())));
        }
        for (int l : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            assertEquals(l, bic.parse(bic.asString(l)));
        }
    }
}