package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Base64Test {
    @Test
    public void onAnField() {
        Wire wire = Wire.newYamlWireOnHeap();
        final UsesBase64 writer = wire.methodWriter(UsesBase64.class);
        final long helloWorld = Base64.INSTANCE.parse("HelloWorld");
        final long byeNow = Base64.INSTANCE.parse("Bye_Now");
        writer.asByte(Byte.MAX_VALUE);
        writer.asShort(Short.MAX_VALUE);
        writer.asInt(Integer.MAX_VALUE);
        writer.asLong(helloWorld);
        writer.send(new Data64(Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, byeNow));

        final String expected = "" +
                "asByte: A_\n" +
                "...\n" +
                "asShort: G__\n" +
                "...\n" +
                "asInt: A_____\n" +
                "...\n" +
                "asLong: HelloWorld\n" +
                "...\n" +
                "send: {\n" +
                "  b: A_,\n" +
                "  s: G__,\n" +
                "  i: A_____,\n" +
                "  data: Bye_Now\n" +
                "}\n" +
                "...\n";
        assertEquals(expected, wire.toString());

        Wire wire2 = Wire.newYamlWireOnHeap();
        final MethodReader reader = wire.methodReader(wire2.methodWriter(UsesBase64.class));
        for (int i = 0; i <= 5; i++)
            assertEquals(i < 5, reader.readOne());

        assertEquals(expected, wire2.toString());
    }

    interface UsesBase64 {
        void asByte(@Base64 byte base64);

        void asShort(@Base64 short base64);

        void asInt(@Base64 int base64);

        void asLong(@Base64 long base64);

        void send(Data64 data64);
    }

    static class Data64 extends SelfDescribingMarshallable {
        @Base64
        byte b;
        @Base64
        short s;
        @Base64
        int i;
        @Base64
        long data;

        public Data64(byte b, short s, int i, long data) {
            this.b = b;
            this.s = s;
            this.i = i;
            this.data = data;
        }
    }
}
