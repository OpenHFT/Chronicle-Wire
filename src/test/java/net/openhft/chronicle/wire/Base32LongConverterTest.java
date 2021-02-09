package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base32LongConverterTest extends WireTestCommon {
    @Test
    public void parse() {
        LongConverter bic = new Base32LongConverter();
        for (String s : ",O,A,L,ZZ,QQ,ABCDEGHIJKLM,5OPQRSTVWXYZ,JZZZZZZZZZZZ".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)));
            assertEquals(s, bic.asString(bic.parse(s.toLowerCase())));
        }

    }
}