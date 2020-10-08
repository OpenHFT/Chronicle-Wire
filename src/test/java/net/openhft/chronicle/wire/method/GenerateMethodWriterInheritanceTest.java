package net.openhft.chronicle.wire.method;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static net.openhft.chronicle.wire.WireType.BINARY;
import static org.junit.Assert.assertFalse;

public class GenerateMethodWriterInheritanceTest {

    @Test
    public void test() {
        AnInterface writer = BINARY.apply(Bytes.elasticByteBuffer())
                .methodWriter(AnInterface.class, ADescendant.class);

        writer.sayHello("hello world");

        // Proxy method writer is constructed in case compilation of generated code failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    @Test
    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/215")
    public void testSameNamedMethod() {
        AnInterface writer = BINARY.apply(Bytes.elasticByteBuffer())
                .methodWriter(AnInterface.class, AnInterfaceSameName.class);

        writer.sayHello("hello world");

        // Proxy method writer is constructed in case compilation of generated code failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    interface AnInterface {
        void sayHello(String name);
    }

    interface AnInterfaceSameName {
        void sayHello(String name);
    }

    interface ADescendant extends AnInterface {
    }
}

