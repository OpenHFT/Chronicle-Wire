package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

interface MockMethods {
    void method1(MockDto dto);

    void method2(MockDto dto);

    void method3(List<MockDto> dtos);

    void list(List<String> strings);
}

@SuppressWarnings("rawtypes")
public class VanillaMethodReaderTest extends WireTestCommon {

    A instance;

    @Test
    public void testMethodReaderWriterMetadata() {
        Bytes b = Bytes.elasticByteBuffer();
        try {
            Wire w = WireType.BINARY.apply(b);
            {
                AListener aListener = w.methodWriter(true, AListener.class);
                A a = new A();
                a.x = 5;
                aListener.a(a);
                // pretend to be system metadata
                aListener.index2index(a);
            }

            MethodReader methodReader = w.methodReader(new AListener() {
                @Override
                public void a(final A a) {
                    VanillaMethodReaderTest.this.instance = a;
                }

                @Override
                public void index2index(A a) {
                    // this should not be called
                    VanillaMethodReaderTest.this.instance = null;
                }
            });
            {
                boolean succeeded = methodReader.readOne();
                assertTrue(succeeded);
                assertEquals(5, this.instance.x);
            }
            {
                boolean succeeded = methodReader.readOne();
                assertTrue(succeeded);
                assertEquals(5, this.instance.x);
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void readMethods() throws IOException {
        Wire wire = new TextWire(BytesUtil.readFile("methods-in.yaml"))
                .useTextDocuments();
        Wire wire2 = new TextWire(Bytes.allocateElasticDirect())
                .useTextDocuments();
        // expected
        Bytes expected = BytesUtil.readFile("methods-in.yaml");
        MockMethods writer = wire2.methodWriter(MockMethods.class);
        MethodReader reader = wire.methodReader(writer);
        for (int i = 0; i < 3; i++) {
            assertTrue(reader.readOne());
            while (wire2.bytes().peekUnsignedByte(wire2.bytes().writePosition() - 1) == ' ')
                wire2.bytes().writeSkip(-1);
        }
        assertFalse(reader.readOne());
        assertEquals(expected.toString().trim().replace("\r", ""), wire2.toString().trim());
    }

    @SuppressWarnings("unused")
    @Test
    public void readMethodsCollections() throws IOException, InterruptedException {
        Wire wire = new TextWire(BytesUtil.readFile("methods-collections-in.yaml"))
                .useTextDocuments();
        Wire wire2 = new TextWire(Bytes.allocateElasticDirect())
                .useTextDocuments();
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        MockMethods mocker = Mocker.queuing(MockMethods.class, "", queue);
        MethodReader reader = wire.methodReader(mocker);
        for (int i = 0; i < 2; i++) {
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
        assertEquals(2, queue.size());
        queue.take();
        assertEquals("method3[[{field1=gidday, field2=1}, {field1=mate, field2=2}]]", queue.take());
    }

    @Test
    public void testSubclasses() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        MRTListener writer = wire.methodWriter(MRTListener.class);
        writer.timed(1234567890_000_000L);
        writer.top(new MRT1("one"));
        writer.method2("one", new MRT1("one"));
        writer.top(new MRT2("one", "two"));
        writer.mid(new MRT1("1"));
        writer.method2("1", new MRT1("1"));
        writer.mid(new MRT2("1", "2"));

        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(MRTListener.class, "subs ", sw));
        for (int i = 0; i < 7; i++) {
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
        String expected = "subs timed[1234567890000000]\n" +
                "subs top[!net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs method2[one, !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs top[!net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: a,\n" +
                "  field2: two\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs method2[1, !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: a,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "]\n";
        String actual = sw.toString().replace("\r", "");
        assertEquals(expected, actual);
    }

    @Test
    public void methodInterceptor() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        MRTListener writer = wire.methodWriterBuilder(MRTListener.class)
                .methodWriterListener((m, a) -> IntStream.range(0, a.length).filter(i -> a[i] instanceof MRT1).forEach(i -> ((MRT1) a[i]).value = "x"))
                .get();
        writer.timed(1234567890L);
        writer.top(new MRT1("one"));
        writer.top(new MRT2("one", "two"));
        writer.mid(new MRT1("1"));
        writer.mid(new MRT2("1", "2"));

        assertEquals("timed: 1234567890\n" +
                "---\n" +
                "top: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: x\n" +
                "}\n" +
                "---\n" +
                "top: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: x,\n" +
                "  field2: two\n" +
                "}\n" +
                "---\n" +
                "mid: {\n" +
                "  field1: \"1\",\n" +
                "  value: x\n" +
                "}\n" +
                "---\n" +
                "mid: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: x,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "---\n", wire.toString());
    }

    @Test
    public void methodInterceptorNull() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        MRTListener writer = wire.methodWriterBuilder(MRTListener.class)
                .build();
        writer.top(new MRT1("one"));
        writer.top(new MRT2("one", "two"));
        writer.mid(new MRT1("1"));
        writer.mid(new MRT2("1", "2"));

        assertEquals("top: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "---\n" +
                "top: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: a,\n" +
                "  field2: two\n" +
                "}\n" +
                "---\n" +
                "mid: {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "---\n" +
                "mid: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: a,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "---\n", wire.toString());
    }

    @Test
    public void testNestedUnknownClass() {
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        MRTListener writer2 = wire2.methodWriter(MRTListener.class);

        String text = "unknown: {\n" +
                "  u: !UnknownClass2 {\n" +
                "    one: 1,\n" +
                "    two: 2.2,\n" +
                "    three: words\n" +
                "  }\n" +
                "}\n" +
                "---\n";
        Wire wire = TextWire.from(text)
                .useTextDocuments();
        MethodReader reader = wire.methodReader(writer2);
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals(text, wire2.toString());
    }

    @Test
    public void testUnknownClass() {
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        MRTListener writer2 = wire2.methodWriter(MRTListener.class);

        String text = "top: !UnknownClass {\n" +
                "  one: 1,\n" +
                "  two: 2.2,\n" +
                "  three: words\n" +
                "}\n" +
                "---\n" +
                "top: {\n" +
                "  one: 11,\n" +
                "  two: 22.2,\n" +
                "  three: many words\n" +
                "}\n" +
                "---\n";
        Wire wire = TextWire.from(text)
                .useTextDocuments();
        MethodReader reader = wire.methodReader(writer2);
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals(text, wire2.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testOverloaded() {
        Map<ExceptionKey, Integer> map = Jvm.recordExceptions();
        try {
            Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap(32));
            Overloaded writer2 = wire2.methodWriter(Overloaded.class);
            Wire wire = TextWire.from("method: [ ]\n");
            MethodReader reader = wire.methodReader(writer2);
            assertNotNull(reader);
//            reader.readOne();

            String s = map.keySet().toString();
            assertTrue(s, s.contains("Unable to support overloaded methods, ignoring one of method"));
        } finally {
            Jvm.resetExceptionHandlers();
        }
    }

    // keep package local.
    interface AListener {
        void a(A a);

        // this pretends to be system metadata
        void index2index(A a);
    }

    interface MRTInterface {

    }

    interface MRTListener {
        void timed(long time);

        void top(MRTInterface mrti);

        void mid(MRT1 mrt1);

        void method2(String key, MRT1 mrt);

        void unknown(NestedUnknown unknown);
    }

    interface Overloaded {
        void method();

        void method(MockDto dto);
    }

    static class A extends SelfDescribingMarshallable {
        int x;
    }

    static class NestedUnknown extends SelfDescribingMarshallable {
        Marshallable u;
    }

    static class MRT1 extends SelfDescribingMarshallable implements MRTInterface {
        final String field1;
        String value = "a";

        MRT1(String field1) {
            this.field1 = field1;
        }
    }

    static class MRT2 extends MRT1 {
        final String field2;

        MRT2(String field1, String field2) {
            super(field1);
            this.field2 = field2;
        }
    }
}

class MockDto extends SelfDescribingMarshallable {
    String field1;
    double field2;
}
