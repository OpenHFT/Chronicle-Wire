package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.io.StringWriter;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
 * Created by peter on 17/05/2017.
 */
public class MethodWriterTest {
    @Test
    public void testSubclasses() {
        Wire wire = new TextWire(Bytes.elasticHeapByteBuffer(256));
        Event writer = wire.methodWriterBuilder(Event.class).genericEvent("event").build();
        writer.event("top", new MethodReaderTest.MRT1("one"));
        writer.event("top", new MethodReaderTest.MRT2("one", "two"));
        writer.event("mid", new MethodReaderTest.MRT1("1"));
        writer.event("mid", new MethodReaderTest.MRT2("1", "2"));

        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(MethodReaderTest.MRTListener.class, "subs ", sw));
        for (int i = 0; i < 4; i++) {
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
        String expected = "subs top[!net.openhft.chronicle.wire.MethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs top[!net.openhft.chronicle.wire.MethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: a,\n" +
                "  field2: two\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.MethodReaderTest$MRT1 {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.MethodReaderTest$MRT2 {\n" +
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
        Wire wire = new TextWire(Bytes.elasticHeapByteBuffer(256));
        HasDefault writer = wire.methodWriter(HasDefault.class);
        writer.callsMethod("hello,world,bye");
        assertEquals("method: [\n" +
                "  hello,\n" +
                "  world,\n" +
                "  bye\n" +
                "]\n" +
                "---\n", wire.toString());

    }

    interface Event {
        void event(String eventName, Object o);
    }

    public interface HasDefault {
        default void callsMethod(String args) {
            method(args.split(","));
        }

        void method(String... args);
    }
}

