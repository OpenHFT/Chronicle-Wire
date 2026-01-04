/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

interface MyInterface<I extends MyInterface<I>> {
    I hello(String hello);

    void terminator();
}

// Test class to validate the behavior of generic methods in the context of Wire operations
class GenericMethodsTest extends WireTestCommon {

    // Test for chained method calls with TextWire
    @Test
    @DisplayName("Writes chained generic method calls to text wire")
    void chainedText() {

        // Create a new TextWire instance with elastic byte allocation
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap(128))
                .useTextDocuments();
        MyInterface<?> top = wire.methodWriter(MyInterface.class);
        assertFalse(Proxy.isProxyClass(top.getClass()),
                "method writer should not be a proxy class");

        // Chain multiple method calls and terminate
        top.hello("hello world").hello("hello world 2").terminator();

        // Assert the expected output from the wire after the method calls
        assertEquals("hello: hello world\n" +
                "hello: hello world 2\n" +
                "terminator: \"\"\n" +
                "...\n", wire.toString(),
                "text wire should contain chained method calls in order");
    }
}
