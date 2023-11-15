/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.openhft.chronicle.wire.WireType.BINARY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Test class for verifying method writer generation and inheritance behaviors in the Chronicle Wire system.
 */
public class GenerateMethodWriterInheritanceTest extends WireTestCommon {

    /**
     * Tests that method writer generation works for the same class in the hierarchy.
     */
    @Test
    public void testSameClassInHierarchy() {
        // Create a new binary wire that uses padding
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Generate a method writer for AnInterface and ADescendant
        final AnInterface writer = wire.methodWriter(AnInterface.class, ADescendant.class);
        assertInstanceOf(MethodWriter.class, writer);

        // Invoke a method on the writer
        writer.sayHello("hello world");

        // Setup a flag to register method calls
        final AtomicBoolean callRegistered = new AtomicBoolean();

        // Create a method reader that listens for method calls
        final MethodReader reader = wire.methodReader(new SameInterfaceImpl(callRegistered));

        // Verify the method call is read and registered
        assertTrue(reader.readOne());
        assertTrue(callRegistered.get());

        // Ensure that the default VanillaMethodReader is not used
        assertFalse(reader instanceof VanillaMethodReader);

        // Ensure that the writer is not a proxy, indicating successful compilation
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    /**
     * Tests that method writer generation handles methods with the same name properly.
     */
    @Test
    public void testSameNamedMethod() {
        // Similar setup as the previous test
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Generate a method writer for AnInterface with a method of the same name from another interface
        final AnInterface writer = wire.methodWriter(AnInterface.class, AnInterfaceSameName.class);
        assertInstanceOf(MethodWriter.class, writer);

        // Same testing process as the previous method
        writer.sayHello("hello world");
        final AtomicBoolean callRegistered = new AtomicBoolean();
        final MethodReader reader = wire.methodReader(new SameMethodNameImpl(callRegistered));

        assertTrue(reader.readOne());
        assertTrue(callRegistered.get());

        // VanillaMethodReader is used in case compilation of generated reader failed.
        assertFalse(reader instanceof VanillaMethodReader);

        // Proxy method writer is constructed in case compilation of generated writer failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    // TODO: same names but different MethodIds should barf

    @Test(expected = MethodWriterValidationException.class)
    public void testDuplicateMethodIds() {
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());

        // Attempt to build a method writer with duplicate method IDs, expecting an exception
        final VanillaMethodWriterBuilder<AnInterfaceMethodId> builder =
            (VanillaMethodWriterBuilder<AnInterfaceMethodId>) wire.methodWriterBuilder(AnInterfaceMethodId.class);
        builder.addInterface(AnInterfaceSameMethodId.class).build();
    }

    // This test is expected to throw a MethodWriterValidationException when trying to generate a method writer for a class
    @Test(expected = MethodWriterValidationException.class)
    public void testGenerateForClass() {
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());

        // Attempt to generate a method writer for a non-interface class, expecting an exception
        wire.methodWriter(GenerateMethodWriterInheritanceTest.class);
    }

    /**
     * Test to ensure that method writers can be generated for interfaces with
     * very long names. This could be a check against the limits of class name lengths
     * in certain dynamic proxy or code generation scenarios.
     */
    @Test
    public void testGenerateForLongGeneratedClassName() {
        // Allocate a new binary wire buffer
        final Wire wire = BINARY.apply(Bytes.allocateElasticOnHeap());

        // Generate a method writer proxy that implements all three interfaces
        Object writer = wire.methodWriter(
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener3.class
        );

        // Assert that the proxy writer instance implements all three interfaces
        assertInstanceOf(NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class, writer);
        assertInstanceOf(NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class, writer);
        assertInstanceOf(NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener3.class, writer);
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

        public SameMethodNameImpl(AtomicBoolean callRegistered) {
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

        public SameInterfaceImpl(AtomicBoolean callRegistered) {
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
