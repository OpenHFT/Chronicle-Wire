package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinaryInTextTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void testBytesFromText() {
        Bytes a = Marshallable.fromString(Bytes.class, "A==");
        assertEquals("A==", a.toString());

        BytesStore a2 = Marshallable.fromString(BytesStore.class, "A==");
        assertEquals("A==", a2.toString());

        Bytes b = Marshallable.fromString(Bytes.class, "!!binary BA==");
        assertEquals("[pos: 0, rlim: 1, wlim: 2147483632, cap: 2147483632 ] ǁ⒋‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", b.toDebugString());

        Bytes b2 = Marshallable.fromString(Bytes.class, "!!binary A1==");
        assertEquals("[pos: 0, rlim: 1, wlim: 2147483632, cap: 2147483632 ] ǁ⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", b2.toDebugString());
    }

    @Test
    public void testReserialize() {
        BIT b = new BIT();
        byte[] a = new byte[5];
        b.b = Bytes.wrapForRead(a);
        b.c = Bytes.wrapForRead(a);
       // System.out.println(b);

        BIT bit = Marshallable.fromString(BIT.class, "{\n" +
                "b: !!binary AAAAAAA=,\n" +
                "c: !!binary CCCCCCCC,\n" +
                "}");
        String bitToString = bit.toString();
        assertTrue(bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                "  b: !!binary AAAAAAA=,\n" +
                "  c: !!binary CCCCCCCC\n" +
                "}\n") ||
                bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                        "  c: !!binary CCCCCCCC,\n" +
                        "  b: !!binary AAAAAAA=\n" +
                        "}\n"));
    }

    @SuppressWarnings("rawtypes")
    static class BIT extends SelfDescribingMarshallable {
        Bytes b;
        BytesStore c;
    }
}
