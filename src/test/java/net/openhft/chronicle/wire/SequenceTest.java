package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


@RunWith(value = Parameterized.class)
public class SequenceTest {

    private final WireType wireType;


    public SequenceTest(WireType wireType) {
        this.wireType = wireType;

    }

    @Parameterized.Parameters
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(

                new Object[]{WireType.BINARY},
                new Object[]{WireType.TEXT}
        );
    }

    static class My extends AbstractMarshallable {
        List<CharSequence> stuff = new ArrayList<>();
        transient List<CharSequence> stuffBuffer = new ArrayList<>();

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read("stuff").sequence(stuff, stuffBuffer, StringBuilder::new);
        }
    }

    @Test
    public void test() {

        My m1 = new My();
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire w1 = wireType.apply(bytes);
        m1.stuff.addAll(Arrays.asList("one", "two", "three"));
        m1.writeMarshallable(w1);

        m1.stuff.clear();
        m1.stuff.addAll(Arrays.asList("four", "five", "six"));
        m1.writeMarshallable(w1);

        m1.stuff.clear();
        m1.stuff.addAll(Arrays.asList("seven", "eight"));
        m1.writeMarshallable(w1);

        {
            My m2 = new My();
            Wire w2 = wireType.apply(bytes);
            m2.readMarshallable(w2);

            Assert.assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    one,\n" +
                    "    two,\n" +
                    "    three\n" +
                    "  ]\n" +
                    "}\n", m2.toString());


            m2.readMarshallable(w2);

            Assert.assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    four,\n" +
                    "    five,\n" +
                    "    six\n" +
                    "  ]\n" +
                    "}\n", m2.toString());


            m2.readMarshallable(w2);

            Assert.assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    seven,\n" +
                    "    eight\n" +
                    "  ]\n" +
                    "}\n", m2.toString());
        }

    }

}
