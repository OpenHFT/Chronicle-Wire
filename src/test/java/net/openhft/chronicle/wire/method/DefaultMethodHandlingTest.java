package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.TextWire;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

interface WithDefault {
    void method1(String text);

    default void method2(String text2) {
    }
}

public class DefaultMethodHandlingTest {
    @Test
    public void withDefault() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());
        WithDefault withDefault = wire.methodWriter(WithDefault.class);
        withDefault.method1("one");
        withDefault.method2("two");
        assertEquals("method1: one\n" +
                "...\n" +
                "method2: two\n" +
                "...\n", wire.toString());
    }
}
