package net.openhft.chronicle.wire;

import org.junit.Test;

import static net.openhft.chronicle.bytes.Bytes.elasticByteBuffer;
import static net.openhft.chronicle.wire.WireType.BINARY;

public class UpdateInterceptorReturnTypeTest {

    @Test
    public void testUpdateInterceptorNoReturnType() {

        BINARY.apply(elasticByteBuffer())
                .methodWriterBuilder(NoReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
    }

    @Test
    public void testUpdateInterceptorWithIntReturnType() {
        BINARY.apply(elasticByteBuffer())
                .methodWriterBuilder(WithIntReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
    }

    @Test
    public void testUpdateInterceptorWithObjectReturnType() {
        BINARY.apply(elasticByteBuffer())
                .methodWriterBuilder(WithObjectReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
    }

    interface NoReturnType {
        void x(String x);
    }

    interface WithIntReturnType {
        int x(String x);
    }

    interface WithObjectReturnType {
        Object x(String x);
    }

    interface WithObjectVoidReturnType {
        Void x(String x);
    }

}