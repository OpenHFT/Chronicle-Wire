package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base64ConverterTest {

    @Test
    public void parse() {
        LongConverter c = Base64Converter.INSTANCE;
        for (String s : ",a,ab,abc,abcd,ab.de,123+56,1234567,12345678,123456789".split(",")) {
            long v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }
}