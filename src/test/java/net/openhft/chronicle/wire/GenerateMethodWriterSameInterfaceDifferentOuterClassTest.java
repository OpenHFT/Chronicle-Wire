package net.openhft.chronicle.wire;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static net.openhft.chronicle.wire.WireType.BINARY;

public class GenerateMethodWriterSameInterfaceDifferentOuterClassTest extends TestCase {

    @Test
    public void test() {

        final Wire wire = BINARY.apply(Bytes.elasticByteBuffer());
        wire.usePadding(true);

        wire
                .methodWriterBuilder(Outer1.InnerInterface.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");

        wire
                .methodWriterBuilder(Outer2.InnerInterface.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .y("hello world");

    }
}

