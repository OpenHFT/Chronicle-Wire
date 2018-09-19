package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

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

    @Test
    public void asString() {
        IntConverter c = Base85IntConverter.INSTANCE;
        Random rand = new Random();
        for (int i = 0; i < 100000; i++) {
            int l = rand.nextInt();
            String s = c.asString(l);
            Assert.assertEquals(s, l, c.parse(s));
        }
    }
}