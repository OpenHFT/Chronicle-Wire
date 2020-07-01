package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

interface MyInterface<I extends MyInterface> {
    I hello(String hello);

    void terminator();
}

public class GenericMethodsTest {
    @Test
    public void chainedText() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();
        MyInterface top = wire.methodWriter(MyInterface.class);

        top.hello("hello world").hello("hello world 2").terminator();

        Assert.assertEquals("hello: hello world\n" +
                "hello: hello world 2\n" +
                "terminator: \"\"\n" +
                "---\n", wire.toString());
    }

}
