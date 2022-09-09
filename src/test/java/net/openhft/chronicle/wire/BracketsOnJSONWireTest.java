package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class BracketsOnJSONWireTest {

    String actual;

    interface Printer {
        void print(String msg);
    }

    @Test
    public void test() {

        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        Wire wire = WireType.JSON_ONLY.apply(t);
        wire.methodWriter(Printer.class)
                .print("hello");

        assertEquals("{\"print\":\"hello\"}", wire.toString());

        wire.methodReader((Printer) msg -> actual = msg).readOne();
        assertEquals("hello", actual);
    }


}
