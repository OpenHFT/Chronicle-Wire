package net.openhft.chronicle.wire.methodwriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MethodWriterProxyTest extends MethodWriterTest {

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
    public void block() {
        super.block();
    }

    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void blockPrimitive() {
        super.blockPrimitive();
    }

    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void blockNoArg() {
        super.blockNoArg();
    }
}
