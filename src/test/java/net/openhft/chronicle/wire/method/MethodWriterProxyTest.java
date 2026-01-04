/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Test class extending MethodWriterTest to test behavior of method writers when using proxies
public class MethodWriterProxyTest extends MethodWriterTest {

    // Method to set up the test environment before each test method
    @BeforeEach
    public void before() {
        // Disable proxy code generation for the duration of the tests
        System.setProperty("disableProxyCodegen", "true");

        // Expect a specific warning message about falling back to proxy method writer
        expectException("Falling back to proxy method writer");
    }

    // Method to clean up and reset the environment after each test method
    @AfterEach
    public void after() {
        // Clear the property to re-enable proxy code generation
        System.clearProperty("disableProxyCodegen");
    }

    // Test method inherited from the parent class but ignored due to a known issue
    @Test
    @Disabled("https://github.com/OpenHFT/Chronicle-Wire/issues/159 - multiOut proxy path")
    @DisplayName("Proxy method writer uses multi-output wiring")
    @Override
    public void multiOut() {
        // Calls the same test method from the parent class
        super.multiOut();
        fail("Disabled multiOut test should not continue past proxy assertions");
    }

    // Test method for testing primitives, ignored on specific conditions and known issues
    @Test
    @Disabled("https://github.com/OpenHFT/Chronicle-Wire/issues/159 - primitives proxy path")
    @DisplayName("Proxy method writer handles primitive argument serialisation")
    @Override
    public void testPrimitives() {
        // Calls the test method for primitives from the parent class
        super.doTestPrimitives(true);
        fail("Disabled primitives test should not continue past proxy assertions");
    }

    // Method to check the type of the writer object in the context of this test class
    @Override
    protected void checkWriterType(Object writer) {
        // Skip the check on Mac ARM architecture
        assumeFalse(Jvm.isMacArm(), "Proxy writer checks are disabled on Mac ARM");

        // Assert that the writer object is a proxy class
        assertTrue(Proxy.isProxyClass(writer.getClass()),
                "Proxy writer should be used when codegen is disabled");
    }
}
