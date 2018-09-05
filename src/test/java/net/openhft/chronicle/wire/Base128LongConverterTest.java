package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base128LongConverterTest {

    @Test
    public void parse() {
        LongConverter c = Base128LongConverter.INSTANCE;
//        System.out.println(c.asString(-1L));
        for (String s : ",a,ab,abc,abcd,abcde,123456,1234567,12345678,123456789,~~~~~~~~~".split(",")) {
            long v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }
}