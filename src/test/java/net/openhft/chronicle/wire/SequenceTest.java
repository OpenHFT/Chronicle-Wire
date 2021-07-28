package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class SequenceTest extends WireTestCommon {

    private final WireType wireType;

    public SequenceTest(WireType wireType) {
        this.wireType = wireType;

    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.BINARY},
                new Object[]{WireType.TEXT},
//                new Object[]{WireType.YAML}, TODO FIX
                new Object[]{WireType.JSON}
        );
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

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    one,\n" +
                    "    two,\n" +
                    "    three\n" +
                    "  ]\n" +
                    "}\n", m2.toString());

            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    four,\n" +
                    "    five,\n" +
                    "    six\n" +
                    "  ]\n" +
                    "}\n", m2.toString());

            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    seven,\n" +
                    "    eight\n" +
                    "  ]\n" +
                    "}\n", m2.toString());
        }
        bytes.releaseLast();
    }

    @Test
    public void readSetAsObject() {
        Bytes bytes = Bytes.allocateElasticOnHeap();
        Wire w1 = wireType.apply(bytes);
        Set<String> value = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        try (DocumentContext dc = w1.writingDocument()) {
            w1.write("list").object(value);
        }
        System.out.println(Wires.fromSizePrefixedBlobs(w1));
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = w1.read("list").object();
            if (wireType == WireType.JSON)
                o = new LinkedHashSet<>((Collection) o);
            assertEquals(value, o);
        }
    }

    @Test
    public void readListAsObject() {
        Bytes bytes = Bytes.allocateElasticOnHeap();
        Wire w1 = wireType.apply(bytes);
        List<String> value = Arrays.asList("a", "b", "c");
        try (DocumentContext dc = w1.writingDocument()) {
            w1.write("list").object(value);
        }
        System.out.println(Wires.fromSizePrefixedBlobs(w1));
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = w1.read("list").object();
            assertEquals(value, o);
        }
    }

    @Test
    public void readMapAsObject() {
        assumeFalse(wireType == WireType.RAW);
        Bytes bytes = Bytes.allocateElasticOnHeap();
        Wire w1 = wireType.apply(bytes);
        Map<String, String> value = new LinkedHashMap<>();
        value.put("a", "aya");
        value.put("b", "bee");
        try (DocumentContext dc = w1.writingDocument()) {
            w1.write("map").object(value);
        }

        System.out.println(Wires.fromSizePrefixedBlobs(w1));
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = w1.read("map").object();
            assertEquals(value, o);
        }
    }

    static class My extends SelfDescribingMarshallable {
        List<CharSequence> stuff = new ArrayList<>();
        transient List<CharSequence> stuffBuffer = new ArrayList<>();

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read("stuff").sequence(stuff, stuffBuffer, StringBuilder::new);
        }
    }
}