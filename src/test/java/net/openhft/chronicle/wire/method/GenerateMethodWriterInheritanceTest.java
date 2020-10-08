package net.openhft.chronicle.wire.method;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;

import java.lang.reflect.Proxy;

import static net.openhft.chronicle.wire.WireType.BINARY;

public class GenerateMethodWriterInheritanceTest extends TestCase {

    public void test() {
        AnInterface writer = BINARY.apply(Bytes.elasticByteBuffer())
                .methodWriter(AnInterface.class, ADescendant.class);

        writer.sayHello("hello world");

        // Proxy method writer is constructed in case compilation of generated code failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    interface AnInterface {
        void sayHello(String name);
    }

    interface ADescendant extends AnInterface {
    }
}

