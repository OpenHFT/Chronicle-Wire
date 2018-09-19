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
        LongHolder lh = new LongHolder();
        lh.hex = 0XFEDCBA9876543210L;
        lh.unsigned = Long.MIN_VALUE;
        lh.timestamp = 0x05432108090a0bL;
        assertEquals("!LongHolder {\n" +
                "  unsigned: 9223372036854775808,\n" +
                "  hex: fedcba9876543210,\n" +
                "  timestamp: 2016-12-08T08:00:31.345163\n" +
                "}\n", lh.toString());
        LongConversionTest.LongHolder lh2 = Marshallable.fromString(lh.toString());
        assertEquals(lh2, lh);
    }

    @Test
    public void method() {
        Wire wire = new TextWire(Bytes.elasticHeapByteBuffer(64));
        LongConversionTest.WriteWithLong write = wire.methodWriter(LongConversionTest.WriteWithLong.class);
        assertSame(write, write.to(0x12345));

        assertEquals("to: 12345\n" +
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
        @LongConversion(UnsignedLongConverter.class)
        long unsigned;
        @LongConversion(HexadecimalLongConverter.class)
        long hex;
        @LongConversion(MicroTimestampLongConverter.class)
        long timestamp;
    }
}
