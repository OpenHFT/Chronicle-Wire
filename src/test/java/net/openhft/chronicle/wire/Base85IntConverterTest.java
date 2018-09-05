package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base85IntConverterTest {

    @Test
    public void parse() {
        IntConverter c = Base85IntConverter.INSTANCE;
        System.out.println(c.asString(-1));
        for (String s : ",a,ab,abc,abcd,ab.de,zzzzz,.Gk<0".split(",")) {
            int v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }
}