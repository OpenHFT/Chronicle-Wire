package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.core.Jvm;
import org.junit.*;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class MethodWriterProxyTest extends MethodWriterTest {
    @BeforeClass
    public void checkNotMac() {
        assumeFalse(Jvm.isMacArm());
    }

    @Before
    public void before() {
        System.setProperty("disableProxyCodegen", "true");
    }

    @After
    public void after() {
        System.clearProperty("disableProxyCodegen");
    }

    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void multiOut() {
        super.multiOut();
    }

    @Test
    public void testPrimitives() {
        super.doTestPrimitives(true);
    }

    @Override
    protected void checkWriterType(Object writer) {
        assertTrue(Proxy.isProxyClass(writer.getClass()));
    }
}

