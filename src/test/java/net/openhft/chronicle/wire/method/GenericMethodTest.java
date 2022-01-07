package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GenericMethodTest extends WireTestCommon {
    final WireType wireType;

    public GenericMethodTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.BINARY},
                {WireType.TEXT},
        });
    }

    @Test
    public void genericArg() {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        final SpecificArg specificArg = wire.methodWriter(SpecificArg.class);
        specificArg.method(new Arg("hi"));
        if (wireType == WireType.TEXT)
            assertEquals("" +
                    "method: {\n" +
                    "  text: hi\n" +
                    "}\n" +
                    "...\n", wire.toString());

        Queue<Arg> queue = new LinkedList<>();
        SpecificArg arg2 = queue::add;
        final MethodReader reader = wire.methodReader(arg2);
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("" +
                "[!net.openhft.chronicle.wire.method.GenericMethodTest$Arg {\n" +
                "  text: hi\n" +
                "}\n" +
                "]", queue.toString());
    }

    @Test
    public void genericReturn() {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        final SpecificReturn specificReturn = wire.methodWriter(SpecificReturn.class);
        specificReturn.to(128).method(new Arg("G'day"));
        if (wireType == WireType.TEXT)
            assertEquals("to: 128\n" +
                    "method: {\n" +
                    "  text: G'day\n" +
                    "}\n" +
                    "...\n", wire.toString());

        Queue<Object> queue = new LinkedList<>();
        SpecificReturn ret2 = d -> {
            queue.add("to " + d);
            return queue::add;
        };
        final MethodReader reader = wire.methodReader(ret2);
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("" +
                "[to 128, !net.openhft.chronicle.wire.method.GenericMethodTest$Arg {\n" +
                "  text: G'day\n" +
                "}\n" +
                "]", queue.toString());

    }

    interface GenericArg<T> {
        void method(T t);
    }

    interface SpecificArg extends GenericArg<Arg> {
        // TODO Known issue that this method needs to be overridden
        @Override
        void method(Arg arg);
    }

    interface GenericReturn<T> {
        T to(long destination);
    }

    interface SpecificReturn extends GenericReturn<SpecificArg> {
        // TODO Known issue that this method needs to be overridden
        @Override
        SpecificArg to(long destination);
    }

    static class Arg extends SelfDescribingMarshallable {
        String text;

        public Arg(String text) {
            this.text = text;
        }
    }
}
