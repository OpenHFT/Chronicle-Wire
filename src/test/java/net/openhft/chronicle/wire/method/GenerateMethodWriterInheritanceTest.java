package net.openhft.chronicle.wire.method;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.Ignore;

import static net.openhft.chronicle.wire.WireType.BINARY;

@Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/215")
public class GenerateMethodWriterInheritanceTest extends TestCase {

    public void test() {

        BINARY.apply(Bytes.elasticByteBuffer())
                .methodWriter(AnInterface.class, ADescendant.class)
                .sayHello("hello world");
    }

    private interface AnInterface {
        void sayHello(String name);
    }

    private interface ADescendant extends AnInterface {
    }
}

