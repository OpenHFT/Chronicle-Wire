package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class Base95LongConverterTest extends WireTestCommon {

    @Test
    public void parse() {
        LongConverter c = Base95LongConverter.INSTANCE;
        // System.out.println(c.asString(-1L));
        for (String s : ",a,ab,abc,abcd,ab.de,123+56,1234567,12345678,123456789,z23456789,0z2345689,<8S@[|bcB".split(",")) {
            long v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }

    @Test
    public void asString() {
        LongConverter c = Base95LongConverter.INSTANCE;
        Random rand = new Random();
        for (int i = 0; i < 100000; i++) {
            long l = (long) Math.random() * Base95LongConverter.MAX_LENGTH;
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
        final LongConverter converter = Base95LongConverter.INSTANCE;
        for (long i = 3; i <= 95 * 95; i++) {
            wire.clear();
            wire.write("a")
                    .writeLong(converter, i);
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });
            final String s = wire.toString();
            try {
                assertEquals(s,
                        i, wire.read("a").readLong(converter));
                wire.read("b").sequence(i, (i2, v) -> {
                    assertEquals((long) i2, v.readLong(converter));
                    assertEquals((long) i2, v.readLong(converter));
                });
            } catch (Throwable t) {
                System.err.println("Failed to parse i: " + i + "\n" + s);
                throw t;
            }
        }
    }
}