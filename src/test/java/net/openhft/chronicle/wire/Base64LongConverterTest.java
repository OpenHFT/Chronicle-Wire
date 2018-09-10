package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class Base64LongConverterTest {

    @Test
    public void parse() {
        LongConverter c = Base64LongConverter.INSTANCE;
        System.out.println(c.asString(-1L));
        for (String s : ",a,ab,abc,abcd,ab.de,123+56,1234567,12345678,123456789,z23456789,z234567890,O++++++++++".split(",")) {
            long v = c.parse(s);
            assertEquals(s, c.asString(v));
        }
    }

    @Test
    public void asString() {
        LongConverter c = Base64LongConverter.INSTANCE;
        Random rand = new Random();
        for (int i = 0; i < 100000; i++) {
            long l = rand.nextLong();
            String s = c.asString(l);
            Assert.assertEquals(s, l, c.parse(s));
        }
    }
}