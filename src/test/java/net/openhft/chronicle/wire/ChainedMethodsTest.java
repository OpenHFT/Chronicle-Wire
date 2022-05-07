package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class ChainedMethodsTest extends WireTestCommon {

    @Test
    public void chainedText() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();
        ITop top = wire.methodWriter(ITop.class);
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");
        assertEquals("mid: mid\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "...\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n" +
                "...\n", wire.toString());

        StringBuilder sb = new StringBuilder();
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    @Test
    public void chainedYaml() {
        YamlWire wire = new YamlWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();
        ITop top = wire.methodWriter(ITop.class);
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");
        assertEquals("mid: mid\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "...\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n" +
                "...\n", wire.toString());

        StringBuilder sb = new StringBuilder();
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    @Test
    public void chainedBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);
        ITop top = wire.methodWriter(ITop.class);
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");

        assertEquals("" +
                "--- !!data #binary\n" +
                "mid: mid\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "# position: 36, header: 1\n" +
                "--- !!data #binary\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n", WireDumper.of(wire).asString());
        StringBuilder sb = new StringBuilder();
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    @Test
    public void chainedBinaryVariousArgsNumber() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);
        ITop top = wire.methodWriter(ITop.class);
        top.midNoArg()
                .next(1)
                .echo("echo-1");

        top.midTwoArgs(5, -7L)
                .next(2)
                .echo("echo-2");

        assertEquals("" +
                        "--- !!data #binary\n" +
                        "midNoArg: \"\"\n" +
                        "next: 1\n" +
                        "echo: echo-1\n" +
                        "# position: 36, header: 1\n" +
                        "--- !!data #binary\n" +
                        "midTwoArgs: [\n" +
                        "  5,\n" +
                        "  !byte -7\n" +
                        "],\n" +
                        "next: 2\n" +
                        "echo: echo-2\n",
                WireDumper.of(wire).asString());

        StringBuilder sb = new StringBuilder();

        ITop implementingOnlyITop = new ITop() {
            @Override
            public IMid mid(String name) {
                throw new UnsupportedOperationException("not supported");
            }

            @Override
            public IMid2 mid2(String name) {
                throw new UnsupportedOperationException("not supported");
            }

            @Override
            public IMid midNoArg() {
                return Mocker.intercepting(IMid.class, "*", sb::append);
            }

            @Override
            public IMid midTwoArgs(int i, long l) {
                return Mocker.intercepting(IMid.class, "*", sb::append);
            }
        };

        MethodReader reader = wire.methodReader(implementingOnlyITop);
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertEquals("*next[1]*echo[echo-1]*next[2]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    @Test
    public void testNestedReturnType() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);
        final NestedStart writer = wire.methodWriter(NestedStart.class);

        assertEquals(false, Proxy.isProxyClass(writer.getClass()));

        writer.start().end();

        assertEquals("--- !!data #binary\n" +
                "start: \"\"\n" +
                "end: \"\"\n", WireDumper.of(wire).asString());
    }

    interface NestedStart {
        NestedEnd start();
    }

    interface NestedEnd {
        void end();
    }
}
