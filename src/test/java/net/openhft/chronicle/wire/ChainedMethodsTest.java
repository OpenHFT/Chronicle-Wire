package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void chainedBinaryVariousArgsNumber() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        ITop top = wire.methodWriter(ITop.class);
        top.midNoArg()
                .next(1)
                .echo("echo-1");

        top.midTwoArgs(5, -7L)
                .next(2)
                .echo("echo-2");

        assertEquals("--- !!data #binary\n" +
                "midNoArg: \"\"\n" +
                "next: 1\n" +
                "echo: echo-1\n" +
                "# position: 43, header: 1\n" +
                "--- !!data #binary\n" +
                "midTwoArgs: [\n" +
                "  !int 5,\n" +
                "  -7\n" +
                "],\n" +
                "next: 2\n" +
                "echo: echo-2\n" , WireDumper.of(wire).asString());

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

    // TODO to be removed before merge
    @Test
    public void testGenerated() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        Object writer = wire.methodWriter(TestClassesStorage.MyInterface1.class, TestClassesStorage.MyInterface2.class);

        TestClassesStorage.MyInterface1 myInterface1 = (TestClassesStorage.MyInterface1)writer;
        TestClassesStorage.MyInterface2 myInterface2 = (TestClassesStorage.MyInterface2)writer;

        myInterface1.call1();
        myInterface1.call2("dw");
        myInterface1.call3(4L, 5);

        List<Boolean> boolList = new ArrayList<>();
        boolList.add(true);
        boolList.add(false);
        myInterface1.call4(new byte[32], 3.5, boolList);
        myInterface1.call5(new ArrayList<>());
        myInterface1.call6(new TestClassesStorage.MyCustomType1(), new TestClassesStorage.MyCustomType2(), 2);

        // not supported by writer
        //myInterface1.call7();

        final TestClassesStorage.DoChain r32 = myInterface1.call8(new TestClassesStorage.MyCustomType1());
        r32.call14();

        final TestClassesStorage.DoChain r33 = myInterface1.call8(new TestClassesStorage.MyCustomType1());
        final TestClassesStorage.DoChain2 doChain2 = r33.call10(true);
        doChain2.call12();

        myInterface2.call9(true, new TestClassesStorage.MyCustomType2());

        System.out.println(WireDumper.of(wire).asString());

        StringBuilder sb = new StringBuilder();
        TestClassesStorage.MyInterface1 impl1 = Mocker.intercepting(
                TestClassesStorage.MyInterface1.class, "*", sb::append);
        TestClassesStorage.MyInterface2 impl2 = Mocker.intercepting(
                TestClassesStorage.MyInterface2.class, "*", sb::append);

        final MethodReader methodReader = wire.methodReader(impl1, impl2);

        while (methodReader.readOne()) {
        }

        System.out.println(sb);
    }

}
