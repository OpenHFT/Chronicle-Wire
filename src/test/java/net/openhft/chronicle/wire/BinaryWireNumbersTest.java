package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
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

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                {2 + 8, (Consumer<ValueOut>) w -> w.int64(Long.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Long.MIN_VALUE)},
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(-(1L << 48)), (Consumer<ValueOut>) w -> w.int64(-(1L << 48))},
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Integer.MIN_VALUE)},
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Short.MIN_VALUE)},
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(Byte.MIN_VALUE), (Consumer<ValueOut>) w -> w.int64(Byte.MIN_VALUE)},
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(-1), (Consumer<ValueOut>) w -> w.int64(-1)},
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(0), (Consumer<ValueOut>) w -> w.int64(0)},
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(Byte.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Byte.MAX_VALUE)},
                {2 + 1, (Consumer<ValueOut>) w -> w.uint8(0xFF), (Consumer<ValueOut>) w -> w.int64(0xFF)},
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Short.MAX_VALUE)},
                {2 + 2, (Consumer<ValueOut>) w -> w.uint16(0xFFFF), (Consumer<ValueOut>) w -> w.int64(0xFFFF)},
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Integer.MAX_VALUE)},
                {2 + 4, (Consumer<ValueOut>) w -> w.uint32(0xFFFFFFFF), (Consumer<ValueOut>) w -> w.int64(0xFFFFFFFF)},
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(1L << 50), (Consumer<ValueOut>) w -> w.int64(1L << 50)},
                {2 + 8, (Consumer<ValueOut>) w -> w.int64(Long.MAX_VALUE), (Consumer<ValueOut>) w -> w.int64(Long.MAX_VALUE)},
                // floating point tests
                {2 + 8, (Consumer<ValueOut>) w -> w.float64(Double.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Double.MIN_VALUE)},
                {2 + 8, (Consumer<ValueOut>) w -> w.float64(Double.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Double.MAX_VALUE)},
                {2 + 8, (Consumer<ValueOut>) w -> w.float32(Float.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Float.MIN_VALUE)},
                {2 + 8, (Consumer<ValueOut>) w -> w.float32(Float.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Float.MAX_VALUE)},
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(-(1L << 48)), (Consumer<ValueOut>) w -> w.float64(-(1L << 48))},
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Integer.MIN_VALUE)},
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Short.MIN_VALUE)},
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(Byte.MIN_VALUE), (Consumer<ValueOut>) w -> w.float64(Byte.MIN_VALUE)},
                {2 + 1, (Consumer<ValueOut>) w -> w.int8(-1), (Consumer<ValueOut>) w -> w.float64(-1)},
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(0), (Consumer<ValueOut>) w -> w.float64(0)},
                {2, (Consumer<ValueOut>) w -> w.wireOut().bytes().writeUnsignedByte(Byte.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Byte.MAX_VALUE)},
                {2 + 1, (Consumer<ValueOut>) w -> w.uint8(0xFF), (Consumer<ValueOut>) w -> w.float64(0xFF)},
                {2 + 2, (Consumer<ValueOut>) w -> w.int16(Short.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Short.MAX_VALUE)},
                {2 + 2, (Consumer<ValueOut>) w -> w.uint16(0xFFFF), (Consumer<ValueOut>) w -> w.float64(0xFFFF)},
                {2 + 4, (Consumer<ValueOut>) w -> w.int32(Integer.MAX_VALUE), (Consumer<ValueOut>) w -> w.float64(Integer.MAX_VALUE)},
                {2 + 4, (Consumer<ValueOut>) w -> w.uint32(0xFFFFFFFF), (Consumer<ValueOut>) w -> w.float64(0xFFFFFFFF)},
                {2 + 4, (Consumer<ValueOut>) w -> w.float32(1L << 50), (Consumer<ValueOut>) w -> w.float64(1L << 50)},
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

    public void test(Consumer<ValueOut> expected, Consumer<ValueOut> perform) {
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
