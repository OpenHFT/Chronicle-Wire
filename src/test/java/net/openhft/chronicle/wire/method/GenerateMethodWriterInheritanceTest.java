/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.openhft.chronicle.wire.WireType.BINARY;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test class for verifying method writer generation and inheritance behaviors in the Chronicle Wire system.
 */
class GenerateMethodWriterInheritanceTest extends WireTestCommon {

    /**
     * Tests that method writer generation works for the same class in the hierarchy.
     */
    @Test
    @DisplayName("Method writer resolves same class in hierarchy")
    void testSameClassInHierarchy() {
        // Create a new binary wire that uses padding
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Generate a method writer for AnInterface and ADescendant
        final AnInterface writer = wire.methodWriter(AnInterface.class, ADescendant.class);
        assertInstanceOf(MethodWriter.class, writer,
                "Method writer should be generated for interface plus descendant");

        // Invoke a method on the writer
        writer.sayHello("hello world");

        // Setup a flag to register method calls
        final AtomicBoolean callRegistered = new AtomicBoolean();

        // Create a method reader that listens for method calls
        final MethodReader reader = wire.methodReader(new SameInterfaceImpl(callRegistered));

        // Verify the method call is read and registered
        assertTrue(reader.readOne(), "Reader should process the sayHello call for descendant interface");
        assertTrue(callRegistered.get(), "Implementation should receive the sayHello call for descendant interface");

        // Ensure that the default VanillaMethodReader is not used
        assertFalse(reader instanceof VanillaMethodReader,
                "Reader should be generated for hierarchy test, not VanillaMethodReader");

        // Ensure that the writer is not a proxy, indicating successful compilation
        assertFalse(Proxy.isProxyClass(writer.getClass()),
                "Writer should be generated for hierarchy test, not a proxy");
    }

    /**
     * Tests that method writer generation handles methods with the same name properly.
     */
    @Test
    @DisplayName("Method writer handles same named methods")
    void testSameNamedMethod() {
        // Similar setup as the previous test
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Generate a method writer for AnInterface with a method of the same name from another interface
        final AnInterface writer = wire.methodWriter(AnInterface.class, AnInterfaceSameName.class);
        assertInstanceOf(MethodWriter.class, writer,
                "Method writer should be generated for same-name interfaces");

        // Same testing process as the previous method
        writer.sayHello("hello world");
        final AtomicBoolean callRegistered = new AtomicBoolean();
        final MethodReader reader = wire.methodReader(new SameMethodNameImpl(callRegistered));

        assertTrue(reader.readOne(), "Reader should process the sayHello call for same-name interfaces");
        assertTrue(callRegistered.get(), "Implementation should receive the sayHello call for same-name interfaces");

        // VanillaMethodReader is used in case compilation of generated reader failed.
        assertFalse(reader instanceof VanillaMethodReader,
                "Reader should be generated for same-name test, not VanillaMethodReader");

        // Proxy method writer is constructed in case compilation of generated writer failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()),
                "Writer should be generated for same-name test, not a proxy");
    }

    // TODO: same names but different MethodIds should barf

    @Test
    @DisplayName("Duplicate MethodId values are rejected by validation")
    void testDuplicateMethodIds() {
        assertThrows(MethodWriterValidationException.class, () -> {
            final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());

            // Attempt to build a method writer with duplicate method IDs, expecting an exception
            final VanillaMethodWriterBuilder<AnInterfaceMethodId> builder =
                    (VanillaMethodWriterBuilder<AnInterfaceMethodId>) wire.methodWriterBuilder(AnInterfaceMethodId.class);
            builder.addInterface(AnInterfaceSameMethodId.class).build();
        }, "Duplicate MethodId values should fail validation");
    }

    // This test is expected to throw a MethodWriterValidationException when trying to generate a method writer for a class
    @Test
    @DisplayName("Method writer rejects concrete class inputs")
    void testGenerateForClass() {
        assertThrows(MethodWriterValidationException.class, () -> {
            final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());

            // Attempt to generate a method writer for a non-interface class, expecting an exception
            wire.methodWriter(GenerateMethodWriterInheritanceTest.class);
        }, "Method writer should reject non-interface types");
    }

    /**
     * Test to ensure that method writers can be generated for interfaces with
     * very long names. This could be a check against the limits of class name lengths
     * in certain dynamic proxy or code generation scenarios.
     */
    @Test
    @DisplayName("Method writer supports long generated class names")
    void testGenerateForLongGeneratedClassName() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for long class name test");

        // Allocate a new binary wire buffer
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());

        // Generate a method writer proxy that implements all three interfaces
        Object writer = wire.methodWriter(
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener3.class
        );

        // Assert that the proxy writer instance implements all three interfaces
        assertInstanceOf(NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class, writer,
                "Writer should implement first long-name interface");
        assertInstanceOf(NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class, writer,
                "Writer should implement second long-name interface");
        assertInstanceOf(NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener3.class, writer,
                "Writer should implement third long-name interface");
    }

    // Interfaces and classes used in the test:

    // Simple interface with a single method for testing basic interaction
    interface AnInterface {
        void sayHello(String name);
    }

    // Interface with the same method name as AnInterface, for testing method name clashes
    interface AnInterfaceSameName {
        void sayHello(String name);
    }

    // Interface with a method annotated with a MethodId, for testing method ID conflicts
    interface AnInterfaceMethodId {
        @MethodId(1)
        void sayHello(String name);
    }

    // Another interface with a different method but the same MethodId as AnInterfaceMethodId
    interface AnInterfaceSameMethodId {
        @MethodId(1)
        void sayHello2(String name);
    }

    // Extension of AnInterface to test method inheritance
    interface ADescendant extends AnInterface {
    }

    // Implementation that combines AnInterface and AnInterfaceSameName to handle method name clashes
    static class SameMethodNameImpl implements AnInterface, AnInterfaceSameName {
        private final AtomicBoolean callRegistered;

        SameMethodNameImpl(AtomicBoolean callRegistered) {
            this.callRegistered = callRegistered;
        }

        @Override
        public void sayHello(String name) {
            callRegistered.set(true);
        }
    }

    // Implementation that combines AnInterface and ADescendant for testing method inheritance
    static class SameInterfaceImpl implements AnInterface, ADescendant {
        private final AtomicBoolean callRegistered;

        SameInterfaceImpl(AtomicBoolean callRegistered) {
            this.callRegistered = callRegistered;
        }

        @Override
        public void sayHello(String name) {
            callRegistered.set(true);
        }
    }

    // Interfaces with long names to test the edge cases of method writer generation
    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1 {

    }

    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2 {

    }

    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener3 {

    }
}
