package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
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

    @Test
    public void longConversion() {
        Sample sample = new Sample();
        sample.strategyId = Base40LongConverter.INSTANCE.parse("TEST");

        final String expectedToString = "!net.openhft.chronicle.wire.Base40LongConverterTest$Sample {\n" +
                "  strategyId: TEST\n" +
                "}\n";

        assertEquals(expectedToString, sample.toString());
    }

    private static class Sample extends SelfDescribingMarshallable {
        @LongConversion(Base40LongConverter.class)
        public long strategyId;
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
        final LongConverter converter = Base40LongConverter.INSTANCE;
        for (long i = 0; i <= 40 * 40; i++) {
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