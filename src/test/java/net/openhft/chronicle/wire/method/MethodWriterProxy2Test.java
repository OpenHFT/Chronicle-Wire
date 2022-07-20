package net.openhft.chronicle.wire.method;

import org.junit.After;
import org.junit.Before;

public class MethodWriterProxy2Test extends MethodWriter2Test {

    @Before
    public void before() {
        System.setProperty("disableProxyCodegen", "true");
        expectException("Falling back to proxy method writer");
    }

    @After
    public void after() {
        System.clearProperty("disableProxyCodegen");
    }
}
