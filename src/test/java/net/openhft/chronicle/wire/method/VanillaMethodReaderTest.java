package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.wire.*;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
            checkReaderType(methodReader);
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
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        // expected
        Bytes expected = BytesUtil.readFile("methods-in.yaml");
        MockMethods writer = wire2.methodWriter(MockMethods.class);
        MethodReader reader = wire.methodReader(writer);
        checkReaderType(reader);
        for (int i = 0; i < 3; i++) {
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
        assertEquals(expected.toString().trim().replace("\r", ""), wire2.toString().trim());
    }

    @SuppressWarnings("unused")
    @Test
    public void readMethodsCollections() throws IOException, InterruptedException {
        Wire wire = new TextWire(BytesUtil.readFile("methods-collections-in.yaml"))
                .useTextDocuments();
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        MockMethods mocker = Mocker.queuing(MockMethods.class, "", queue);
        MethodReader reader = wire.methodReader(mocker);
        checkReaderType(reader);
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
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap())
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
        checkReaderType(reader);
        for (int i = 0; i < 7; i++) {
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
        String expected = "subs timed[1234567890000000]\n" +
                "subs top[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs method2[one, !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs top[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: a,\n" +
                "  field2: two\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs method2[1, !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: a,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "]\n";
        String actual = sw.toString().replace("\r", "");
        assertEquals(expected, actual);
    }


    @Test
    public void methodInterceptorNull() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        MRTListener writer = wire.methodWriterBuilder(MRTListener.class)
                .build();
        writer.top(new MRT1("one"));
        writer.top(new MRT2("one", "two"));
        writer.mid(new MRT1("1"));
        writer.mid(new MRT2("1", "2"));

        assertEquals("top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "...\n" +
                "top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: a,\n" +
                "  field2: two\n" +
                "}\n" +
                "...\n" +
                "mid: {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "...\n" +
                "mid: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: a,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "...\n", wire.toString());
    }

    @Test
    public void testNestedUnknownClass() {
        Wires.GENERATE_TUPLES = true;

        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        MRTListener writer2 = wire2.methodWriter(MRTListener.class);

        String text = "unknown: {\n" +
                "  u: !UnknownClass2 {\n" +
                "    one: 1,\n" +
                "    two: 2.2,\n" +
                "    three: words\n" +
                "  }\n" +
                "}\n" +
                "...\n";
        Wire wire = TextWire.from(text)
                .useTextDocuments();
        MethodReader reader = wire.methodReader(writer2);
        checkReaderType(reader);
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals(text, wire2.toString());
    }

    @Test
    public void testUnknownClass() {
        Wires.GENERATE_TUPLES = true;

        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        MRTListener writer2 = wire2.methodWriter(MRTListener.class);

        String text = "top: !UnknownClass {\n" +
                "  one: 1,\n" +
                "  two: 2.2,\n" +
                "  three: words\n" +
                "}\n" +
                "...\n" +
                "top: {\n" +
                "  one: 11,\n" +
                "  two: 22.2,\n" +
                "  three: many words\n" +
                "}\n" +
                "...\n";
        Wire wire = TextWire.from(text)
                .useTextDocuments();
        MethodReader reader = wire.methodReader(writer2);
        checkReaderType(reader);
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals(text, wire2.toString());
    }

    @Test
    public void testMessageHistoryCleared() {
        try {
            Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
            final long sourceIndex = 2L;
            MessageHistory.get().reset(1, sourceIndex);

            wire.write(MethodReader.HISTORY).marshallable(MessageHistory.get());
            MRTListener writer = wire.methodWriter(MRTListener.class);
            writer.timed(1234L);

            MethodReader reader = wire.methodReader(Mocker.intercepting(MRTListener.class, (s, objects) -> {
                assertEquals("timed", s);
                assertEquals(1, MessageHistory.get().sources());
                assertEquals(sourceIndex, MessageHistory.get().sourceIndex(0));
            }, null));
            checkReaderType(reader);
            assertTrue(reader.readOne());
            assertFalse(reader.readOne());
        } finally {
            MessageHistory.set(null);
        }
    }

    @After
    public void resetGenerateTuples() {
        Wires.GENERATE_TUPLES = false;
    }

    @Test(expected = IllegalStateException.class)
    public void testOverloaded() {
        Jvm.recordExceptions();
        try {
            Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap(32));
            Overloaded writer2 = wire2.methodWriter(Overloaded.class);
            Wire wire = TextWire.from("method: [ ]\n");
            wire.methodReader(writer2);
        } finally {
            Jvm.resetExceptionHandlers();
        }
    }

    private void checkReaderType(MethodReader reader) {
        assertFalse(Proxy.isProxyClass(reader.getClass()));
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
    @Comment("field1 comment")
    String field1;
    @Comment("field2 comment")
    double field2;
}
