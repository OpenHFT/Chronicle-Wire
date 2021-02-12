package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class Base85LongConverterTest extends WireTestCommon {

    private static final CharSequence TEST_STRING = "world";

    @Test
    public void parse() {
        LongConverter c = Base85LongConverter.INSTANCE;
        // System.out.println(c.asString(-1L));
        for (String s : ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0".split(",")) {
            long v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }

    @Test
    public void asString() {
        LongConverter c = Base85LongConverter.INSTANCE;
        Random rand = new Random();
        for (int i = 0; i < 100000; i++) {
            long l = (long) Math.random() * Base85LongConverter.MAX_LENGTH;
            String s = c.asString(l);
            Assert.assertEquals(s, l, c.parse(s));
        }
    }

    @Test
    public void testAppend() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            final Base85LongConverter idLongConverter = new Base85LongConverter();
            final long helloWorld = idLongConverter.parse(TEST_STRING);
            idLongConverter.append(b, helloWorld);
            assertEquals(TEST_STRING, b.toString());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testAppendWithExistingData() {
        final Bytes<?> b = Bytes.elasticByteBuffer().append("hello");
        try {
            final Base85LongConverter idLongConverter = new Base85LongConverter();
            final long helloWorld = idLongConverter.parse(TEST_STRING);
            idLongConverter.append(b, helloWorld);
            assertEquals("hello" + TEST_STRING, b.toString());
        } finally {
            b.releaseLast();
        }
    }

}