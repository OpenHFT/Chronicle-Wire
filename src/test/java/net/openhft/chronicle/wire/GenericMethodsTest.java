package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static junit.framework.TestCase.assertFalse;

interface MyInterface<I extends MyInterface> {
    I hello(String hello);

    void terminator();
}

public class GenericMethodsTest extends WireTestCommon {
    @Test
    public void chainedText() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();
        MyInterface top = wire.methodWriter(MyInterface.class);
        assertFalse(Proxy.isProxyClass(top.getClass()));

        top.hello("hello world").hello("hello world 2").terminator();

        Assert.assertEquals("hello: hello world\n" +
                "hello: hello world 2\n" +
                "terminator: \"\"\n" +
                "...\n", wire.toString());
    }
}
