package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 25/05/15.
 */

@RunWith(value = Parameterized.class)
public class BinaryWireNumbersTest {
    private final int len;
    private final Consumer<ValueOut> expected;
    private final Consumer<ValueOut> perform;
    private static final float VAL1 = 12345678901234567.0f;



    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{

                {2 + 4, (Consumer<ValueOut>) w -> w.float32(-(1L << 48L)), (Consumer<ValueOut>) w -> w.int64(-(1L << 48L))},    //0
                {2 + 8, (Consumer<ValueOut>) w -> w.int64(Long.MIN_VALUE+1), (Consumer<ValueOut>) w -> w.int64(Long.MIN_VALUE+1)},  //1
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(-VAL1), (Consumer<ValueOut>) w -> w.int64((long) -VAL1)},//2
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(VAL1), (Consumer<ValueOut>) w -> w.int64((long) VAL1)},//3
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Integer.MIN_VALUE)},//4
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Short.MIN_VALUE)},//5
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(Byte.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Byte.MIN_VALUE)},//6
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(-1), (Consumer<ValueOut>) w -> w.int64(-1)},//7
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(0), (Consumer<ValueOut>) w -> w.int64(0)}, //8
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(Byte.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Byte.MAX_VALUE)}, //9
                {2 + 1, (Consumer<ValueOut>) w -> w.uint8(0xFF), (Consumer<ValueOut>) w -> w.int64(0xFF)}, //10
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Short.MAX_VALUE)},// 11
                {2 + 2, (Consumer<ValueOut>) w -> w.uint16(0xFFFF), (Consumer<ValueOut>) w -> w.int64(0xFFFF)}, //12
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Integer.MAX_VALUE)},//13
                {2 + 4, (Consumer<ValueOut>) w -> w.uint32(0xFFFFFFFFL), (Consumer<ValueOut>) w -> w.int64(0xFFFFFFFFL)},//14
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(1L << 50), (Consumer<ValueOut>) w -> w.int64(1L << 50)},//15
                {2 + 8, (Consumer<ValueOut>) w -> w.int64(Long.MAX_VALUE-1), (Consumer<ValueOut>) w -> w.int64(Long.MAX_VALUE-1)}, //16
                // floating point tests
                {2 + 8, (Consumer<ValueOut>) w -> w.float64(Double.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Double.MIN_VALUE)}, //17
                {2 + 8, (Consumer<ValueOut>) w -> w.float64(Double.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Double.MAX_VALUE)}, //18
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(Float.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Float.MIN_VALUE)}, //19
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(Float.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Float.MAX_VALUE)}, //20
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(-(1L << 48)), (Consumer<ValueOut>) w -> w.float64(-(1L << 48))},//21
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Integer.MIN_VALUE)},//22
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Short.MIN_VALUE)},//23
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(Byte.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Byte.MIN_VALUE)},//24
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(-1), (Consumer<ValueOut>) w -> w.float64(-1)},//25
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(0), (Consumer<ValueOut>) w -> w.float64(0)},//26
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(Byte.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Byte.MAX_VALUE)},//27
                {2 + 1, (Consumer<ValueOut>) w -> w.uint8(0xFF), (Consumer<ValueOut>) w -> w.float64(0xFF)},//28
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Short.MAX_VALUE)},//29
                {2 + 2, (Consumer<ValueOut>) w -> w.uint16(0xFFFF), (Consumer<ValueOut>) w -> w.float64(0xFFFF)},//30
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Integer.MAX_VALUE)},//31
                {2 + 4, (Consumer<ValueOut>) w -> w.uint32(0xFFFFFFFFL), (Consumer<ValueOut>) w -> w.float64(0xFFFFFFFFL)},//32
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(1L << 50), (Consumer<ValueOut>) w -> w.float64(1L << 50)},//33
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(Long.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Long.MIN_VALUE)},  //34
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(Long.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Long.MAX_VALUE)},
        });
    }

    public BinaryWireNumbersTest(int len, Consumer<ValueOut> expected, Consumer<ValueOut> perform) {
        this.len = len;
        this.expected = expected;
        this.perform = perform;
    }

    @Test
    public void doTest() {
        test(expected, perform);
    }

    public void test(@NotNull Consumer<ValueOut> expected, @NotNull Consumer<ValueOut> perform) {
        Bytes bytes1 = nativeBytes();
        Wire wire1 = new BinaryWire(bytes1, true, false, false);
        expected.accept(wire1.write());
        bytes1.flip();

        assertEquals("Length for fixed length doesn't match for " + TextWire.asText(wire1), len, bytes1.remaining());

        Bytes bytes2 = nativeBytes();
        Wire wire2 = new BinaryWire(bytes2);
        perform.accept(wire2.write());
        bytes2.flip();

        assertEquals("Lengths for variable length expected " + bytes1
                        + " and actual " + bytes2 + " don't match for " + TextWire.asText(wire1),
                bytes1.remaining(), bytes2.remaining());
        if (!bytes1.toString().equals(bytes2.toString()))
            System.out.println("Format doesn't match for " + TextWire.asText(wire2));
    }

}
