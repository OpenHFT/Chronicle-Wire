package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static net.openhft.chronicle.wire.WireType.BINARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@RunWith(Parameterized.class)
public class UpdateInterceptorReturnTypeTest extends WireTestCommon {
    @Parameterized.Parameter
    public boolean disableProxyCodegen;

    @Parameterized.Parameters(name = DISABLE_WRITER_PROXY_CODEGEN + "={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    @Before
    public void setUp() {
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, String.valueOf(disableProxyCodegen));
    }

    @After
    public void cleanUp() {
        System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
    }

    @Test
    public void testUpdateInterceptorNoReturnType() {

        createWire()
                .methodWriterBuilder(NoReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
    }

    static Wire createWire() {
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);
        return wire;
    }

    @Test
    public void testUpdateInterceptorWithIntReturnType() {
        int value = createWire()
                .methodWriterBuilder(WithIntReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .x("hello world");
        assertEquals(0, value);
    }

    @Test
    public void testUpdateInterceptorWithObjectReturnType() {
        final WithObjectReturnType mw = createWire()
                .methodWriterBuilder(WithObjectReturnType.class)
                .updateInterceptor((methodName, t) -> true)
                .build();
        Object value = mw.x("hello world");
        assertSame(mw, value);
        assertEquals(disableProxyCodegen, Proxy.isProxyClass(mw.getClass()));
    }

    @Test
    public void testUpdateInterceptorWithLadderByQtyListener() {
        createWire()
                .methodWriterBuilder(LadderByQtyListener.class)
                .updateInterceptor((methodName, t) -> true)
                .build()
                .ladderByQty("a ladder");
    }

    public interface LadderByQtyListener {
        void ladderByQty(String ladder);

        default void lbq(String name, String ladder) {
            ladderByQty(ladder);
        }

        default boolean ignoreMethodBasedOnFirstArg(String methodName, String ladderDefinitionName) {
            return false;
        }
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