/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// Test class extending MethodReaderArgumentsRecycleTest to test behavior of method readers when using proxies
class MethodReaderArgumentsRecycleProxyTest extends MethodReaderArgumentsRecycleTest {
    @Override
    @BeforeEach
    void setUp() {
        // Disable proxy code generation for the duration of the tests
        System.setProperty("disableReaderProxyCodegen", "true");
        super.setUp();
    }

    @AfterEach
    void after() {
        // Clear the property to re-enable proxy code generation
        System.clearProperty("disableReaderProxyCodegen");
    }
}
