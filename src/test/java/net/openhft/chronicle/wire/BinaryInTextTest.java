package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class BinaryInTextTest {
    @Test
    public void testBytesFromText() {
        Bytes a = Marshallable.fromString(Bytes.class, "A==");
        assertEquals("A==", a.toString());

        BytesStore a2 = Marshallable.fromString(BytesStore.class, "A==");
        assertEquals("A==", a2.toString());

        Bytes b = Marshallable.fromString(Bytes.class, "!!binary BA==");
        assertEquals("[pos: 0, rlim: 1, wlim: 8EiB, cap: 8EiB ] ǁ⒋‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", b.toDebugString());

        Bytes b2 = Marshallable.fromString(Bytes.class, "!!binary A1==");
        assertEquals("[pos: 0, rlim: 1, wlim: 8EiB, cap: 8EiB ] ǁ⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", b2.toDebugString());
    }

    @Test
    public void testReserialize() {
        BIT b = new BIT();
        byte[] a = new byte[5];
        b.b = Bytes.wrapForRead(a);
        b.c = Bytes.wrapForRead(a);
        System.out.println(b);

        BIT bit = Marshallable.fromString(BIT.class, "{\n" +
                "b: !!binary AAAAAAA=,\n" +
                "c: !!binary CCCCCCCC,\n" +
                "}");
        assertEquals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                "  b: !!binary AAAAAAA=,\n" +
                "  c: !!binary CCCCCCCC\n" +
                "}\n", bit.toString());
    }

    static class BIT extends AbstractMarshallable {
        Bytes b;
        BytesStore c;
    }
}
