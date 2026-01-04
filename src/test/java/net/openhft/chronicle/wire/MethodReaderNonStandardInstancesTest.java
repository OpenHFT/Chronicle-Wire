/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.BooleanConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies that MethodReader can handle non-standard instances like anonymous classes,
 * core classes, and lambda expressions.
 * <p>
 * This class provides tests for non-standard instances being passed to the MethodReader.
 * It extends the WireTestCommon which provides utilities for monitoring thread and exception behaviors during tests.
 */
public class MethodReaderNonStandardInstancesTest extends WireTestCommon {

    /**
     * Test case to verify that an anonymous class can be passed to MethodReader.
     * It sets up a writer, writes a message, then uses a MethodReader to read and process the message using an anonymous class implementation.
     */
    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Anonymous class can be used with MethodReader")
    public void testAnonymousClassCanBePassedToMethodReader() {
        // Initialization of the wire with padding
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        // Create a writer for the MyInterface
        MyInterface writer = wire.methodWriter(MyInterface.class);

        writer.call();

        // Use AtomicBoolean to capture results from the reader
        AtomicBoolean b = new AtomicBoolean();

        // Create a MethodReader that uses an anonymous class implementation
        MethodReader reader = wire.methodReader(new MyInterface() {
            @Override
            public void call() {
                b.set(true);
            }
        });

        // Assertions to ensure the reader is of the expected type and it reads and processes the message correctly
        assertFalse(reader instanceof VanillaMethodReader,
                "MethodReader should use generated code for anonymous class");

        assertTrue(reader.readOne(), "Anonymous class reader should process the single written call");
        assertTrue(b.get(), "Anonymous class should receive the call");
    }

    /**
     * Test case to verify that a core class can be passed to MethodReader.
     * Similar to the above test but uses a BooleanConsumer core class.
     */
    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Core class can be used with MethodReader")
    public void testCoreClassCanBePassedToMethodReader() throws Exception {
        // Initialization of the wire with padding
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        // Create a writer for the BooleanConsumer interface
        BooleanConsumer writer = wire.methodWriter(BooleanConsumer.class);

        writer.accept(true);

        // Use AtomicBoolean to capture results from the reader
        AtomicBoolean b = new AtomicBoolean();

        // Create a MethodReader that uses a BooleanConsumer implementation
        MethodReader reader = wire.methodReader(new BooleanConsumer() {
            @Override
            public void accept(Boolean value) {
                b.set(value);
            }
        });

        // Assertions to ensure the reader is of the expected type and it reads and processes the message correctly
        assertFalse(reader instanceof VanillaMethodReader,
                "MethodReader should use generated code for core classes");

        assertTrue(reader.readOne(), "Core class reader should process the single written call");
        assertTrue(b.get(), "BooleanConsumer should receive the call");
    }

    /**
     * Test case to verify that a lambda expression can be passed to MethodReader.
     * It demonstrates how a lambda can be used to implement the reading functionality.
     */
    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Lambda can be used with MethodReader")
    public void testLambdaCanBePassedToMethodReader() {
        // Initialization of the wire with padding
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        // Create a writer for the MyInterface
        MyInterface writer = wire.methodWriter(MyInterface.class);

        writer.call();

        // Use AtomicBoolean to capture results from the reader
        AtomicBoolean b = new AtomicBoolean();

        // Create a MethodReader that uses a lambda expression for the reading functionality
        MethodReader reader = wire.methodReader((MyInterface) () -> b.set(true));

        // Assertions to ensure the reader is of the expected type and it reads and processes the message correctly
        assertFalse(reader instanceof VanillaMethodReader,
                "MethodReader should use generated code for lambda");

        assertTrue(reader.readOne(), "Lambda reader should process the single written call");
        assertTrue(b.get(), "Lambda should receive the call");
    }

    /**
     * Simple interface for testing.
     * It provides a single call method which the tests use for writing and reading messages.
     */
    interface MyInterface {
        void call();
    }
}
