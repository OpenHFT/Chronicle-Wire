package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class Base128LongConverterTest extends WireTestCommon {

    @Test
    public void parse() {
        LongConverter c = Base128LongConverter.INSTANCE;
        // System.out.println(c.asString(-1L));
        for (String s : ",a,ab,abc,abcd,abcde,123456,1234567,12345678,123456789,~~~~~~~~~".split(",")) {
            long v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }

    @Test
    public void asString() {
        LongConverter c = Base128LongConverter.INSTANCE;
        Random rand = new Random();
        for (int i = 0; i < 100000; i++) {
            long l = (long) Math.random() * Base128LongConverter.MAX_LENGTH;
            String s = c.asString(l);
            Assert.assertEquals(s, l, c.parse(s));
        }
    }

    @Test
    public void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    @Test
    public void allSafeCharsYamlWire() {
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    private void allSafeChars(Wire wire) {
        final LongConverter converter = Base128LongConverter.INSTANCE;
        for (long i = 0; i <= 128 * 128; i++) {
            wire.clear();
            wire.write("a").writeLong(converter, i);
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });
            assertEquals(wire.toString(),
                    i, wire.read("a").readLong(converter));
            wire.read("b").sequence(i, (i2, v) -> {
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }

}