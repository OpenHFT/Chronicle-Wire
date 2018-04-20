package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class IntConversionTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(IntHolder.class);
    }

    @Test
    public void dto() {
        assertEquals("!IntHolder {\n" +
                "  number: \"1234\"\n" +
                "}\n", new IntHolder(0x1234).toString());
        IntHolder ih = Marshallable.fromString(new IntHolder(1234).toString());
        assertEquals(1234, ih.number);
    }

    @Test
    public void method() {
        Wire wire = new TextWire(Bytes.elasticHeapByteBuffer(64));
        WriteWithInt write = wire.methodWriter(WriteWithInt.class);
        assertSame(write, write.to(0x12345));

        assertEquals("to: \"12345\"\n" +
                "---\n", wire.toString());

        StringWriter sw = new StringWriter();
        WriteWithInt read = Mocker.logging(WriteWithInt.class, "", sw);
        wire.methodReader(read).readOne();

        assertEquals("to[12345]\n", sw.toString().replaceAll("\r", ""));
    }

    interface WriteWithInt {
        WriteWithInt to(@IntConversion(HexadecimalIntConverter.class) int x);
    }

    static class IntHolder extends AbstractMarshallable {
        @IntConversion(HexadecimalIntConverter.class)
        int number;

        public IntHolder(int number) {
            this.number = number;
        }

        public int number() {
            return number;
        }

        public IntHolder number(int number) {
            this.number = number;
            return this;
        }
    }
}
