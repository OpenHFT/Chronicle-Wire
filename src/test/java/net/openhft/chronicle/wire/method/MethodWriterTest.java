package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.*;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;

import static junit.framework.TestCase.assertFalse;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MethodWriterTest extends WireTestCommon {
    @Test
    public void testSubclasses() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256));
        Event writer = wire.methodWriterBuilder(Event.class).genericEvent("event").build();
        writer.event("top", new VanillaMethodReaderTest.MRT1("one"));
        writer.event("top", new VanillaMethodReaderTest.MRT2("one", "two"));
        writer.event("mid", new VanillaMethodReaderTest.MRT1("1"));
        writer.event("mid", new VanillaMethodReaderTest.MRT2("1", "2"));

        /**
         * top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {
         *   field1: one,
         *   value: a
         * }
         * ---
         * top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {
         *   field1: one,
         *   value: a,
         *   field2: two
         * }
         * ---
         * mid: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {
         *   field1: "1",
         *   value: a
         * }
         * ---
         * mid: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {
         *   field1: "1",
         *   value: a,
         *   field2: "2"
         * }
         * ---
         */
        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(VanillaMethodReaderTest.MRTListener.class, "subs ", sw));
        for (int i = 0; i < 4; i++) {
            assertTrue(reader.readOne());
        }

        assertFalse(reader.readOne());
        String expected = "subs top[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
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
    public void testDefault() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        HasDefault writer = wire.methodWriter(HasDefault.class);
        writer.callToDefaultMethod("hello world");

        Assert.assertTrue(wire.toString().startsWith("callToDefaultMethod: hello world"));
    }

    @Test
    public void multiOut() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());
        Event event = wire.methodWriter(Event.class);
        event.event("one", "one");
        BinaryWire wire2 = new BinaryWire(new HexDumpBytes());
        ((MethodWriter) event).marshallableOut(wire2);
        event.event("two", "two");
        assertEquals("event: [\n" +
                "  one,\n" +
                "  one\n" +
                "]\n" +
                "...\n", wire.toString());
        assertEquals("" +
                "14 00 00 00                                     # msg-length\n" +
                "b9 05 65 76 65 6e 74 82 08 00 00 00             # event\n" +
                "e3 74 77 6f                                     # two\n" +
                "e3 74 77 6f                                     # two\n", wire2.bytes().toHexString());
        wire2.bytes().releaseLast();
    }

    @Test
    public void ignoreStatic() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256));
        Closeable writer = wire.methodWriter(Closeable.class);
        Closeable.closeQuietly(writer);
        assertEquals("", wire.toString());
    }

    @Test
    public void testNoArgs() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        NoArgs writer = wire.methodWriter(NoArgs.class);
        writer.methodOne();
        writer.methodTwo();
        assertEquals("methodOne: \"\"\n" +
                "...\n" +
                "methodTwo: \"\"\n" +
                "...\n", wire.toString());
        NoArgs mock = createMock(NoArgs.class);
        mock.methodOne();
        mock.methodTwo();
        EasyMock.replay(mock);
        MethodReader reader = wire.methodReader(mock);
        for (int i = 0; i < 3; i++)
            assertEquals(i < 2, reader.readOne());
        verify(mock);
    }

    @Test
    public void testUpdateListener() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();

        final StringBuilder value = new StringBuilder();

        StringMethod instance = wire.methodWriterBuilder(StringMethod.class).updateInterceptor((methodName, t) -> {
            value.append(t);
            return true;
        }).build();

        String expected = "hello world";
        instance.method(expected);
        Assert.assertEquals(expected, value.toString());

        Assert.assertTrue(wire.toString().startsWith("method: hello world\n" +
                "...\n"));
    }

    @Test
    public void testUpdateListenerCheckUpdateInterceptorReturnValue() {
        final Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();

        StringMethod instance = wire.methodWriterBuilder(StringMethod.class).updateInterceptor((methodName, t) -> false).build();
        instance.method(" this should not be written because the return value above is false");

        Assert.assertEquals("", wire.toString());
    }

    @Test
    public void testMicroTS() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        HasMicroTS writer = wire.methodWriter(HasMicroTS.class);
        long now = 1532251709775811L; //TimeProvider.get().currentTimeMicros();
//        System.out.println(now);
        MicroTS microTS = new MicroTS();
        microTS.timeUS = now;
        writer.microTS(microTS);
        assertEquals("microTS: {\n" +
                "  timeUS: 2018-07-22T09:28:29.775811\n" +
                "}\n" +
                "...\n", wire.toString());
        HasMicroTS mock = createMock(HasMicroTS.class);
        MethodReader reader = wire.methodReader(mock);
        mock.microTS(microTS);
        replay(mock);
        for (int i = 0; i < 2; i++)
            assertEquals(i < 1, reader.readOne());
        verify(mock);
    }

    @Test
    public void testPrimitives() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();
        Args writer = wire.methodWriter(Args.class);
        writer.primitives(true, (byte) 1, (short) 2, 3, 4, '5', 6, 7, "8", "9");
        assertEquals("primitives: [\n" +
                "  true,\n" +
                "  1,\n" +
                "  2,\n" +
                "  !int 3,\n" +
                "  4,\n" +
                "  \"5\",\n" +
                "  !float 6.0,\n" +
                "  7.0,\n" +
                "  \"8\",\n" +
                "  \"9\"\n" +
                "]\n" +
                "...\n", wire.toString());
        Args mock = createMock(Args.class);
        mock.primitives(true, (byte) 1, (short) 2, 3, 4, '5', 6, 7, "8", "9");
        EasyMock.replay(mock);
        MethodReader reader = wire.methodReader(mock);
        for (int i = 0; i < 2; i++)
            assertEquals(i < 1, reader.readOne());
        verify(mock);
    }

    @FunctionalInterface
    interface Event {
        void event(String eventName, Object o);
    }

    @FunctionalInterface
    public interface HasDefault {
        default void callToDefaultMethod(String value) {

        }

        void method(String args);
    }

    public interface StringMethod {
        void method(String value);
    }

    public interface NoArgs {
        void methodOne();

        void methodTwo();
    }

    public interface Args {
        void primitives(boolean n, byte b, short s, int i, long l, char c, float f, double d, String s1, CharSequence s2);
    }

    public interface HasMicroTS {
        void microTS(MicroTS microTS);
    }

    static class MicroTS extends SelfDescribingMarshallable {
        @LongConversion(MicroTimestampLongConverter.class)
        long timeUS;
    }
}

