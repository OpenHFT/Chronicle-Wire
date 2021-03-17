package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.internal.BytesFieldInfo;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.assertEquals;

public class EmbeddedBytesMarshallableTest {
    @Test
    public void ebm() {
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM.class);
        EBM e1 = new EBM();
        e1.number = Base85IntConverter.INSTANCE.parse("Hello");
        e1.a.append("a12345678901234567890123456789");
        e1.b.append("a1234567890123456789abc");
        e1.c.append("a1234567890");
        final String expected = "!EBM {\n" +
                "  number: Hello,\n" +
                "  a: a12345678901234567890123456789,\n" +
                "  b: a1234567890123456789abc,\n" +
                "  c: a1234567890\n" +
                "}\n";
        assertEquals(expected, e1.toString());
        Bytes bytes = new HexDumpBytes();
        e1.writeMarshallable(bytes);
        assertEquals("00 00 03 07 1e 61 31 32 33 34 35 36 37 38 39 30\n" +
                "31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36\n" +
                "37 38 39 00 17 61 31 32 33 34 35 36 37 38 39 30\n" +
                "31 32 33 34 35 36 37 38 39 61 62 63 c4 5f 74 4c\n" +
                "0b 61 31 32 33 34 35 36 37 38 39 30\n", bytes.toHexString());
        EBM e2 = new EBM();
        e2.readMarshallable(bytes);
        assertEquals(expected, e2.toString());
        bytes.releaseLast();
    }

    static class SelfDescribingTriviallyCopyable extends SelfDescribingMarshallable {
        @FieldGroup("header")
        transient int description = BytesFieldInfo.lookup(getClass()).description();
    }

    static class EBM extends SelfDescribingTriviallyCopyable {
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM.class).description();
        static final int[] range = BytesUtil.triviallyCopyableRange(EBM.class);
        public static final int LENGTH = range[1] - range[0];
        public static final int START = range[0];

        @IntConversion(Base85IntConverter.class)
        int number;
        @FieldGroup("a")
        transient long a0, a1, a2, a3;
        @FieldGroup("b")
        transient long b0, b1, b2;
        @FieldGroup("c")
        transient int c0, c1, c3;
        Bytes a = Bytes.forFieldGroup(this, "a");
        Bytes b = Bytes.forFieldGroup(this, "b");
        Bytes c = Bytes.forFieldGroup(this, "c");

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            int description0 = bytes.readInt();
            if (description0 != DESCRIPTION)
                throw new UnsupportedOperationException("TODO FIX");
            bytes.unsafeReadObject(this, START + 4, LENGTH - 4);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            bytes.unsafeWriteObject(this, START, LENGTH);
        }
    }
}
