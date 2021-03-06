package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base40LongConverterTest extends WireTestCommon {
    @Test
    public void parse() {
        Base40LongConverter bic = Base40LongConverter.UPPER;
        for (String s : ",A,0,ZZ,99,ABCDEF,012345,ZZZZZZZZZZZZ,999999999999".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)));
        }
    }

    @Test
    public void parseLower() {
        Base40LongConverter bic = Base40LongConverter.LOWER;
        for (String s : ",a,0,zz,99,abcdef,012345,zzzzzzzzzzzz,999999999999".split(",")) {
            assertEquals(s, bic.asString(bic.parse(s)));
        }

    }
}