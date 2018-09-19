package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.AbstractBytesMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class BytesMarshallableTest {
    private final WireType wireType;

    public BytesMarshallableTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.BINARY_LIGHT}
        );
    }

    private Wire createWire() {
        return wireType.apply(Bytes.elasticHeapByteBuffer(64));
    }

    @Test
    public void primitiveDto() {
        Wire wire = createWire();

        PrimDto dto1 = PrimDto.init(1);
        wire.write("prim").marshallable(dto1);

        ScalarDto sdto1 = ScalarDto.init(1);
        wire.write("scalar").marshallable(sdto1);

        String expected = "Unknown wire type";
        switch (wireType) {
            case TEXT:
                expected = "[pos: 0, rlim: 159, wlim: 8EiB, cap: 8EiB ] ǁprim: {⒑  flag: true,⒑  s8: 1,⒑  ch: \"\\x01\",⒑  s16: 1,⒑  s32: 1,⒑  s64: 1,⒑  f32: 1.0,⒑  f64: 1.0⒑}⒑scalar: {⒑  text: Hello1,⒑  buffer: bye 1,⒑  bytes: hi 1⒑}⒑‡٠٠٠٠٠٠٠٠٠";
                break;
            case BINARY_LIGHT:
                expected = "[pos: 0, rlim: 69, wlim: 8EiB, cap: 8EiB ] ǁÄprim\\u0082\\u001D٠٠٠Y⒈⒈⒈٠⒈٠٠٠⒈٠٠٠٠٠٠٠٠٠\\u0080?٠٠٠٠٠٠ð?Æscalar\\u0082⒙٠٠٠⒍Hello1⒌bye 1⒋hi 1‡٠٠٠٠٠٠٠٠٠٠٠";
                break;
        }
        assertEquals(expected, wire.bytes().toDebugString());

        PrimDto dto2 = new PrimDto();
        ScalarDto sdto2 = new ScalarDto();

        for (int i = 0; i < 2; i++) {
            wire.bytes().readPosition(0);

            wire.read("prim").marshallable(dto2);
            assertEquals(dto1, dto2);

            wire.read("scalar").marshallable(sdto2);
            assertEquals(sdto1, sdto2);
        }
    }

    @Test
    public void primitiveDto2() {
        Wire wire = createWire();

        PrimDto2 dto1 = PrimDto2.init(1);
        wire.write("prim").marshallable(dto1);

        ScalarDto2 sdto1 = ScalarDto2.init(1);
        wire.write("scalar").marshallable(sdto1);

        String expected = "Unknown wire type";
        switch (wireType) {
            case TEXT:
                expected = "[pos: 0, rlim: 159, wlim: 8EiB, cap: 8EiB ] ǁprim: {⒑  flag: true,⒑  s8: 1,⒑  ch: \"\\x01\",⒑  s16: 1,⒑  s32: 1,⒑  s64: 1,⒑  f32: 1.0,⒑  f64: 1.0⒑}⒑scalar: {⒑  text: Hello1,⒑  buffer: bye 1,⒑  bytes: hi 1⒑}⒑‡٠٠٠٠٠٠٠٠٠";
                break;

            case BINARY_LIGHT:
                expected = "[pos: 0, rlim: 50, wlim: 8EiB, cap: 8EiB ] ǁÄprim\\u0082⒑٠٠٠Y⒈⒈⒈⒈⒈\\u009F|\\u009F|Æscalar\\u0082⒙٠٠٠⒍Hello1⒌bye 1⒋hi 1‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠";
                break;
        }
        assertEquals(expected, wire.bytes().toDebugString());

        PrimDto2 dto2 = new PrimDto2();
        ScalarDto2 sdto2 = new ScalarDto2();

        for (int i = 0; i < 2; i++) {
            wire.bytes().readPosition(0);

            wire.read("prim").marshallable(dto2);
            assertEquals(dto1, dto2);

            wire.read("scalar").marshallable(sdto2);
            assertEquals(sdto1, sdto2);
        }

        ClassAliasPool.CLASS_ALIASES.addAlias(PrimDto2.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(ScalarDto2.class);

        assertEquals("!PrimDto2 {\n" +
                "  flag: true,\n" +
                "  s8: 1,\n" +
                "  ch: \"\\x01\",\n" +
                "  s16: 1,\n" +
                "  s32: 1,\n" +
                "  s64: 1,\n" +
                "  f32: 1.0,\n" +
                "  f64: 1.0\n" +
                "}\n", dto2.toString());

        assertEquals("!ScalarDto2 {\n" +
                "  text: Hello1,\n" +
                "  buffer: bye 1,\n" +
                "  bytes: hi 1\n" +
                "}\n", sdto2.toString());
    }

    static class PrimDto extends AbstractBytesMarshallable {
        boolean flag;
        byte s8;
        char ch;
        short s16;
        int s32;
        long s64;
        float f32;
        double f64;

        static PrimDto init(int i) {
            return init(i, new PrimDto());
        }

        static <T extends PrimDto> T init(int i, T d) {
            d.flag = i % 2 != 0;
            d.s8 = (byte) i;
            d.ch = (char) i;
            d.s16 = (short) i;
            d.s32 = i;
            d.s64 = i * i * i;
            d.f32 = d.s32;
            d.f64 = d.s64;
            return d;
        }
    }

    static class PrimDto2 extends PrimDto {
        static PrimDto2 init(int i) {
            return init(i, new PrimDto2());
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException {
            flag = bytes.readBoolean();
            s8 = bytes.readByte();
            ch = (char) Maths.toUInt16(bytes.readStopBit());
            s16 = Maths.toInt16(bytes.readStopBit());
            s32 = Maths.toInt32(bytes.readStopBit());
            s64 = bytes.readStopBit();
            f32 = (float) bytes.readStopBitDouble();
            f64 = bytes.readStopBitDouble();
        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.writeBoolean(flag);
            bytes.writeByte(s8);
            bytes.writeStopBit(ch);
            bytes.writeStopBit(s16);
            bytes.writeStopBit(s32);
            bytes.writeStopBit(s64);
            bytes.writeStopBit(f32);
            bytes.writeStopBit(f64);
        }
    }

    static class ScalarDto extends AbstractBytesMarshallable {
        String text;
        StringBuilder buffer;
        Bytes bytes;

        static ScalarDto init(int i) {
            return init(i, new ScalarDto());
        }

        static <D extends ScalarDto> D init(int i, D d) {
            d.text = "Hello" + i;
            d.buffer = new StringBuilder("bye " + i);
            d.bytes = Bytes.elasticHeapByteBuffer(8).append("hi ").append(i);
            return d;
        }
    }

    static class ScalarDto2 extends ScalarDto {

        static ScalarDto2 init(int i) {
            return init(i, new ScalarDto2());
        }

        @Override
        public void readMarshallable(BytesIn in) throws IORuntimeException {
            text = in.read8bit();
            if (buffer == null) buffer = new StringBuilder();
            in.read8bit(buffer);
            if (bytes == null) bytes = Bytes.elasticHeapByteBuffer(8);
            in.read8bit(bytes);
        }

        @Override
        public void writeMarshallable(BytesOut out) {
            out.write8bit(text);
            out.write8bit(buffer);
            out.write8bit(bytes);
        }
    }
}
