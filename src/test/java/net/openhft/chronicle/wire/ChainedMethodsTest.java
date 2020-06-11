package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import static org.junit.Assert.*;

interface ITop {
    IMid mid(String name);

    IMid2 mid2(String name);
}

interface IMid {
    ILast next(long x);
}

interface IMid2 {
    ILast next2(String a);
}

interface ILast {
    void echo(String text);
}

public class ChainedMethodsTest extends WireTestCommon {
    @Test
    public void chainedText() {
        TextWire wire = new TextWire(Bytes.elasticHeapByteBuffer(128))
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
                "---\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n" +
                "---\n", wire.toString());

        StringBuilder sb = new StringBuilder();
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    @Test
    public void chainedYaml() {
        YamlWire wire = new YamlWire(Bytes.elasticHeapByteBuffer(128))
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
                "---\n" +
                "mid2: mid2\n" +
                "next2: word\n" +
                "echo: echo-2\n" +
                "---\n", wire.toString());

        StringBuilder sb = new StringBuilder();
        MethodReader reader = wire.methodReader(Mocker.intercepting(ITop.class, "*", sb::append));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertEquals("*mid[mid]*next[1]*echo[echo-1]*mid2[mid2]*next2[word]*echo[echo-2]", sb.toString());
        assertFalse(reader.readOne());
    }

    @Test
    public void chainedBinary() {
        Wire wire = new BinaryWire(Bytes.elasticHeapByteBuffer(128));
        ITop top = wire.methodWriter(ITop.class);
        top.mid("mid")
                .next(1)
                .echo("echo-1");
        top.mid2("mid2")
                .next2("word")
                .echo("echo-2");

        assertEquals("--- !!data #binary\n" +
                "mid: mid\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "# position: 41, header: 1\n" +
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
}
