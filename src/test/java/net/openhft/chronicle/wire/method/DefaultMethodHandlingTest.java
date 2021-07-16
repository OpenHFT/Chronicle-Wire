package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

interface WithDefault {
    void method1(String text);

    default void method2(String text2) {
        throw new UnsupportedOperationException();
    }
}

public class DefaultMethodHandlingTest extends WireTestCommon {
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

        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(WithDefault.class, "", sw));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("method1[one]\n" +
                "method2[two]\n", sw.toString().replace("\r", ""));
    }
}
