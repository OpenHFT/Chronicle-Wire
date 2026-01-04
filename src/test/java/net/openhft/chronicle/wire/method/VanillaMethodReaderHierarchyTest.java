/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class extending WireTestCommon to validate the behavior of method readers
 * with different levels of class/interface hierarchy in Chronicle Wire.
 */
class VanillaMethodReaderHierarchyTest extends WireTestCommon {
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

    /**
     * Tests method writing and reading using a simple interface implementation.
     */
    @Test
    @DisplayName("Method reader handles simple interface calls")
    void testInterface() {
        Simple simple = queue::add;
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading using a descendant of a simple interface.
     */
    @Test
    @DisplayName("Method reader handles descendant interface calls")
    void testInterfaceDescend() {
        SimpleDescendant simple = queue::add;
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with a concrete class implementation of a descendant interface.
     */
    @Test
    @DisplayName("Method reader handles descendant class implementations")
    void testDescendantClass() {
        SimpleDescendant simple = new SimpleDescendantClass(queue);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with an abstract class that implements a descendant interface.
     * This test addresses a specific issue (referenced by a GitHub issue link).
     */
    @Test
    @DisplayName("Method reader handles abstract descendant classes")
    void testDescendantAbstractClass() {
        // this was the problem - https://github.com/OpenHFT/Chronicle-Wire/issues/154
        SimpleDescendant simple = new SimpleDescendantClass2(queue);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with a class that extends another class and implements the same interface.
     */
    @Test
    @DisplayName("Method reader handles classes extending same interface")
    void testDescendantExtendsSameInterface() {
        SimpleDescendant simple = new SimpleDescendantClass3(queue);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with duck typing - implementing multiple interfaces with the same method.
     */
    @Test
    @DisplayName("Method reader supports duck typing interfaces")
    void testDuckTyping() {
        DuckTyping simple = new DuckTyping(queue);
        checkWriteRead(simple);
    }

    /**
     * Helper method to check the method writing and reading functionality.
     *
     * @param simple An implementation of the Simple interface.
     */
    private void checkWriteRead(Simple simple) throws InvalidMarshallableException {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(32));
        Simple writer = wire.methodWriter(Simple.class);
        MethodReader reader = wire.methodReader(simple);
        final String superMario = "Mario";
        writer.hello(superMario);
        // writer =    "hello: Mario\n...\n"

        assertTrue(reader.readOne(), "Reader should process the hello call");
        assertEquals(1, queue.size(), "Queue should contain one entry");
        assertEquals(superMario, queue.poll(), "Queue should contain the hello payload");
    }

    interface Simple {
        void hello(String name);
    }

    interface SimpleSameMethod {
        void hello(String name);
    }

    interface SimpleDescendant extends Simple {
    }

    private static class SimpleDescendantClass implements SimpleDescendant {
        private final BlockingQueue<String> queue;

        SimpleDescendantClass(BlockingQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void hello(String name) {
            queue.add(name);
        }
    }

    private abstract static class SimpleAbstractDescendantClass implements SimpleDescendant {
    }

    private static class SimpleDescendantClass2 extends SimpleAbstractDescendantClass {
        private final BlockingQueue<String> queue;

        SimpleDescendantClass2(BlockingQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void hello(String name) {
            queue.add(name);
        }
    }

    private static class SimpleDescendantClass3 extends SimpleDescendantClass2 implements Simple {
        SimpleDescendantClass3(BlockingQueue<String> queue) {
            super(queue);
        }
    }

    private static class DuckTyping extends SimpleDescendantClass2 implements Simple, SimpleSameMethod {
        DuckTyping(BlockingQueue<String> queue) {
            super(queue);
        }
    }
}
