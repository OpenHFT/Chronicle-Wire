package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base32LongConverterTest {
    @Test
    public void parse() {
        LongConverter bic = new Base32LongConverter();
        for (String s : ",O,A,L,ZZ,QQ,ABCDEFGHIJKLM,5OPQRSTUVWXYZ,JZZZZZZZZZZZZ".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)));
            assertEquals(s, bic.asString(bic.parse(s.toLowerCase())));
        }
        for (long l : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            assertEquals(l, bic.parse(bic.asString(l)));
        }
    }
}