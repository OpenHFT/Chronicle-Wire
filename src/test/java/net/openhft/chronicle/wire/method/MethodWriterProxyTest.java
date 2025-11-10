//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.core.Jvm;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

// Test class extending MethodWriterTest to test behavior of method writers when using proxies
public class MethodWriterProxyTest extends MethodWriterTest {

    // Method to set up the test environment before each test method
    @Before
    public void before() {
        // Disable proxy code generation for the duration of the tests
        System.setProperty("disableProxyCodegen", "true");

        // Expect a specific warning message about falling back to proxy method writer
        expectException("Falling back to proxy method writer");
    }

    // Method to clean up and reset the environment after each test method
    @After
    public void after() {
        // Clear the property to re-enable proxy code generation
        System.clearProperty("disableProxyCodegen");
    }

    // Test method inherited from the parent class but ignored due to a known issue
    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void multiOut() {
        // Calls the same test method from the parent class
        super.multiOut();
        fail();
    }

    // Test method for testing primitives, ignored on specific conditions and known issues
    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/159")
    @Test
    public void testPrimitives() {
        // Calls the test method for primitives from the parent class
        super.doTestPrimitives(true);
        fail();
    }

    // Method to check the type of the writer object in the context of this test class
    @Override
    protected void checkWriterType(Object writer) {
        // Skip the check on Mac ARM architecture
        assumeFalse(Jvm.isMacArm());

        // Assert that the writer object is a proxy class
        assertTrue(Proxy.isProxyClass(writer.getClass()));
    }
}
