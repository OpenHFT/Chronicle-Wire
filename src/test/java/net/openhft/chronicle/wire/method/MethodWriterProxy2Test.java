/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import org.junit.After;
import org.junit.Before;

// Test class extending MethodWriter2Test to evaluate behavior of method writers with enforced proxy generation
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class MethodWriterProxy2Test extends MethodWriter2Test {

    // Set up the environment before each test
    @Before
    public void before() {
        // Enforce proxy generation by disabling proxy code generation
        System.setProperty("disableProxyCodegen", "true");

        // Expect a warning message indicating the use of a proxy method writer
        expectException("Falling back to proxy method writer");
    }

    // Clean up and reset the environment after each test
    @After
    public void after() {
        // Clear the property to revert to the default method writer generation behavior
        System.clearProperty("disableProxyCodegen");
    }
}
