package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SequenceTest {

    static class My extends AbstractMarshallable {
        List stuff = new ArrayList<>();
    }

    @Test
    public void test() {

        My m1 = new My();
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire w1 = WireType.BINARY.apply(bytes);
        m1.stuff.addAll(Arrays.asList("one", "two", "three"));
        m1.writeMarshallable(w1);

        My m2 = new My();
        Wire w2 = WireType.BINARY.apply(bytes);
        m2.readMarshallable(w2);

        Assert.assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                "  stuff: [\n" +
                "    one,\n" +
                "    two,\n" +
                "    three\n" +
                "  ]\n" +
                "}\n", m2.toString());


    }

}
