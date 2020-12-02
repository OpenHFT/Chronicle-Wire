package net.openhft.chronicle.wire.method;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MethodWriterProxyTest extends MethodWriterTest {
    @Before
    public void before() {
        System.setProperty("disableProxyCodegen", "true");
        System.setProperty("disableReaderProxyCodegen", "true");
    }

    @After
    public void after() {
        System.clearProperty("disableProxyCodegen");
        System.clearProperty("disableReaderProxyCodegen");
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
}

