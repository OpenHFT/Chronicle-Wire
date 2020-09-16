package net.openhft.chronicle.wire;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;

import static net.openhft.chronicle.wire.WireType.BINARY;

public class GenerateMethodWriterTest extends TestCase {

    public void testInnerInterfaceClass() {

        BINARY.apply(Bytes.elasticByteBuffer())
                .methodWriterBuilder(InnerInterface.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
    }

    interface InnerInterface {
        void x(String message);
    }
}