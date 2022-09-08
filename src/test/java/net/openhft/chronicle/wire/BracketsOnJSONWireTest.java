package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class BracketsOnJSONWireTest {

    String actual;

    interface Printer {
        void print(String msg);
    }

    @Test
    public void test() {

        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        Wire wire = WireType.JSON_ONLY.apply(t);
        wire.methodWriter(Printer.class).print("hello");

        Assert.assertEquals("{", "" + (char) wire.bytes().peekUnsignedByte());

        wire.methodReader((Printer) msg -> actual = msg).readOne();
        Assert.assertEquals("hello", actual);
    }


}
