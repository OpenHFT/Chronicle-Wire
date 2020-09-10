package net.openhft.chronicle.wire.method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MethodWriterProxy2Test extends MethodWriter2Test {

    @Before
    public void before() {
        System.setProperty("disableProxyCodegen", "true");
    }

    @After
    public void after() {
        System.clearProperty("disableProxyCodegen");
    }

    @Test
    public void block() {
        super.block();
    }

    @Test
    public void blockPrimitive() {
        super.blockPrimitive();
    }

    @Test
    public void blockNoArg() {
        super.blockNoArg();
    }
}
