package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class LongConversionTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongHolder.class);
    }

    @Test
    public void dto() {
        assertEquals("!LongHolder {\n" +
                "  number: \"1234\"\n" +
                "}\n", new LongHolder(0x1234).toString());
        LongConversionTest.LongHolder ih = Marshallable.fromString(
                new LongConversionTest.LongHolder(1234).toString());
        assertEquals(1234, ih.number);

    }

    @Test
    public void method() {
        Wire wire = new TextWire(Bytes.elasticHeapByteBuffer(64));
        LongConversionTest.WriteWithLong write = wire.methodWriter(LongConversionTest.WriteWithLong.class);
        assertSame(write, write.to(0x12345));

        assertEquals("to: \"12345\"\n" +
                "---\n", wire.toString());

        StringWriter sw = new StringWriter();
        LongConversionTest.WriteWithLong read = Mocker.logging(LongConversionTest.WriteWithLong.class, "", sw);
        wire.methodReader(read).readOne();

        assertEquals("to[12345]\n", sw.toString().replaceAll("\r", ""));
    }

    interface WriteWithLong {
        LongConversionTest.WriteWithLong to(@LongConversion(HexadecimalLongConverter.class) int x);
    }

    static class LongHolder extends AbstractMarshallable {
        @LongConversion(HexadecimalLongConverter.class)
        long number;

        public LongHolder(long number) {
            this.number = number;
        }

        public long number() {
            return number;
        }

        public LongHolder number(long number) {
            this.number = number;
            return this;
        }
    }
}
