package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.io.Closeable;
import org.easymock.EasyMock;
import org.junit.Ignore;
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
        System.out.println("");
        writer.event("top", new VanillaMethodReaderTest.MRT1("one"));
        writer.event("top", new VanillaMethodReaderTest.MRT2("one", "two"));
        writer.event("mid", new VanillaMethodReaderTest.MRT1("1"));
        writer.event("mid", new VanillaMethodReaderTest.MRT2("1", "2"));

        /**
         * top: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {
         *   field1: one,
         *   value: a
         * }
         * ---
         * top: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {
         *   field1: one,
         *   value: a,
         *   field2: two
         * }
         * ---
         * mid: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {
         *   field1: "1",
         *   value: a
         * }
         * ---
         * mid: !net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {
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

        ValueOut v;

        assertFalse(reader.readOne());
        String expected = "subs top[!net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT1 {\n" +
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
                "subs mid[!net.openhft.chronicle.wire.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: a,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "]\n";
        String actual = sw.toString().replace("\r", "");
        assertEquals(expected, actual);
    }

    @Ignore("TODO FIX")
    @Test
    public void testDefault() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();
        HasDefault writer = wire.methodWriter(HasDefault.class);

        // MethodWriter records an invocation on the default method
        // callsMethod of the _proxy_, not the interface
        // this should work for replaying events back through a VanillaMethodReader.
        // Change made in Chronicle-Core https://github.com/OpenHFT/Chronicle-Core/commit/86c532d20a304c990cb2a82ebd84ffb355660fd3
        writer.callsMethod("hello,world,bye");
        assertEquals("method: [\n" +
                "  hello,\n" +
                "  world,\n" +
                "  bye\n" +
                "]\n" +
                "---\n", wire.toString());
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
                "---\n" +
                "methodTwo: \"\"\n" +
                "---\n", wire.toString());
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
                "---\n", wire.toString());
        HasMicroTS mock = createMock(HasMicroTS.class);
        MethodReader reader = wire.methodReader(mock);
        mock.microTS(microTS);
        replay(mock);
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
        default void callsMethod(String args) {
            method(args.split(","));
        }

        void method(String... args);
    }

    public interface NoArgs {
        void methodOne();

        void methodTwo();
    }

    public interface HasMicroTS {
        void microTS(MicroTS microTS);
    }

    static class MicroTS extends SelfDescribingMarshallable {
        @LongConversion(MicroTimestampLongConverter.class)
        long timeUS;
    }
}

