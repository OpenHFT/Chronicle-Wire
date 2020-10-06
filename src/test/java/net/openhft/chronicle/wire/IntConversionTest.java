package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

public class IntConversionTest extends WireTestCommon {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(IntHolder.class);
    }

    @Test
    public void dto() {
        assertEquals("!IntHolder {\n" +
                "  number: 1234\n" +
                "}\n", new IntHolder(0x1234).toString());
        IntHolder ih = Marshallable.fromString(new IntHolder(1234).toString());
        assertEquals(1234, ih.number);

        ih.number = 0xDEAF;
        assertEquals("!IntHolder {\n" +
                "  number: deaf\n" +
                "}\n", ih.toString());
        IntHolder ih2 = Marshallable.fromString(ih.toString());
        assertEquals(ih2, ih);
    }

    @Test
    public void method() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(64))
                .useTextDocuments();
        WriteWithInt write = wire.methodWriter(WriteWithInt.class);
        assertSame(write, write.to(0x12345));

        assertEquals("to: 12345\n", wire.toString());

        StringWriter sw = new StringWriter();
        WriteWithInt read = Mocker.logging(WriteWithInt.class, "", sw);
        wire.methodReader(read).readOne();

        assertEquals(String.format("to[%d]\n", 0x12345), sw.toString().replaceAll("\r", ""));
    }

    @Test
    public void unsigned() {
        UnsignedHolder uh = new UnsignedHolder();
        uh.u8 = -1;
        uh.u16 = -1;
        uh.u32 = -1;
        assertEquals("!net.openhft.chronicle.wire.IntConversionTest$UnsignedHolder {\n" +
                "  u8: 255,\n" +
                "  u16: 65535,\n" +
                "  u32: 4294967295\n" +
                "}\n", uh.toString());

        UnsignedHolder uh2 = Marshallable.fromString(uh.toString());
        assertEquals(uh2, uh);
    }

    @Test
    public void twoArgumentsConversion() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(128));

        final TwoArgumentsConversion writer = wire.methodWriter(TwoArgumentsConversion.class);

        writer.to(Integer.MIN_VALUE / 3, Long.MAX_VALUE / 2);

        StringBuilder sb = new StringBuilder();

        assertEquals("to: [\n" +
                "  7-DBEN,\n" +
                "  3fffffffffffffff\n" +
                "]\n" +
                "...\n", wire.toString());

        final MethodReader reader = wire.methodReader(Mocker.intercepting(
                TwoArgumentsConversion.class, "*", sb::append));
        assertTrue(reader.readOne());

        assertEquals("*to[-715827882, 4611686018427387903]", sb.toString());
    }

    public interface TwoArgumentsConversion {
        void to(@IntConversion(Base40IntConverter.class) int i, @LongConversion(HexadecimalLongConverter.class) long l);
    }

    public interface WriteWithInt {
        WriteWithInt to(@IntConversion(HexadecimalIntConverter.class) int x);
    }

    static class IntHolder extends SelfDescribingMarshallable {
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

    static class UnsignedHolder extends SelfDescribingMarshallable {
        @IntConversion(UnsignedIntConverter.class)
        public byte u8;
        @IntConversion(UnsignedIntConverter.class)
        public short u16;
        @IntConversion(UnsignedIntConverter.class)
        public int u32;

    }
}
