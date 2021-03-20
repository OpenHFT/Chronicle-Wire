package net.openhft.chronicle.wire;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;

import static net.openhft.chronicle.wire.WireType.BINARY;

public class GenerateMethodWriterSameInterfaceDifferentOuterClassTest extends TestCase {

    public void test() {

        BINARY.apply(Bytes.elasticByteBuffer())
                .methodWriterBuilder(Outer1.InnerInterface.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");

        BINARY.apply(Bytes.elasticByteBuffer())
                .methodWriterBuilder(Outer2.InnerInterface.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .y("hello world");

    }
}

