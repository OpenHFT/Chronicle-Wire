/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Interface defining default behavior for an object that can perform operations
 * with method1 being abstract and method2 providing a default implementation.
 */
interface WithDefault {
    // Abstract method that must be implemented by a concrete class.
    void method1(String text);

    // Default method that throws UnsupportedOperationException if not overridden.
    default void method2(String text2) {
        throw new UnsupportedOperationException();
    }
}
/**
 * Test class for validating the handling of default methods in the WithDefault interface.
 * Extends WireTestCommon for common wire testing utilities.
 */
class DefaultMethodHandlingTest extends WireTestCommon {

    /**
     * Executes tests for method writers and readers with the specified WireType.
     *
     * @param wireType The WireType to use for the test.
     */
    private static boolean doTest(WireType wireType) {
        // Allocate a new wire buffer and create a method writer for WithDefault.
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        WithDefault withDefault = wire.methodWriter(WithDefault.class);

        // Write method calls to the wire.
        withDefault.method1("one");
        withDefault.method2("two");

        // Assert the wire's string representation matches the expected output.
        assertEquals("method1: one\n" +
                "...\n" +
                "method2: two\n" +
                "...\n", wire.toString(),
                "Wire output should include method1 and method2 in order");

        // Create a StringWriter to capture logging output.
        StringWriter sw = new StringWriter();

        // Create a method reader that logs to the StringWriter.
        MethodReader reader = wire.methodReader(Mocker.logging(WithDefault.class, "", sw));

        // Read the method calls and assert the expected outcomes.
        assertTrue(reader.readOne(), "Reader should process method1");
        assertTrue(reader.readOne(), "Reader should process method2");
        assertFalse(reader.readOne(), "Reader should have no more methods");

        // Assert that the logged output matches the expected string.
        assertEquals("method1[one]\n" +
                "method2[two]\n", sw.toString().replace("\r", ""),
                "Logged output should capture method1 and method2 calls");
        return true;
    }

    /**
     * Tests the method writers and readers using TEXT wire format.
     */
    @Test
    @DisplayName("Default method handling on TEXT wire")
    void withDefault() {
        assertTrue(doTest(WireType.TEXT), "Default method handling should succeed on TEXT wire");
    }

    /**
     * Tests the method writers and readers using YAML_ONLY wire format.
     */
    @Test
    @DisplayName("Default method handling on YAML_ONLY wire")
    void withDefaultYaml() {
        assertTrue(doTest(WireType.YAML_ONLY), "Default method handling should succeed on YAML_ONLY wire");
    }
}
