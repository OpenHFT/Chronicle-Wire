//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class extending WireTestCommon to validate the behavior of method readers
 * with different levels of class/interface hierarchy in Chronicle Wire.
 */
public class VanillaMethodReaderHierarchyTest extends WireTestCommon {
    private BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

    /**
     * Tests method writing and reading using a simple interface implementation.
     */
    @Test
    public void testInterface() {
        Simple simple = name -> queue.add(name);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading using a descendant of a simple interface.
     */
    @Test
    public void testInterfaceDescend() {
        SimpleDescendant simple = name -> queue.add(name);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with a concrete class implementation of a descendant interface.
     */
    @Test
    public void testDescendantClass() {
        SimpleDescendant simple = new SimpleDescendantClass(queue);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with an abstract class that implements a descendant interface.
     * This test addresses a specific issue (referenced by a GitHub issue link).
     */
    @Test
    public void testDescendantAbstractClass() {
        // this was the problem - https://github.com/OpenHFT/Chronicle-Wire/issues/154
        SimpleDescendant simple = new SimpleDescendantClass2(queue);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with a class that extends another class and implements the same interface.
     */
    @Test
    public void testDescendantExtendsSameInterface() {
        SimpleDescendant simple = new SimpleDescendantClass3(queue);
        checkWriteRead(simple);
    }

    /**
     * Tests method writing and reading with duck typing - implementing multiple interfaces with the same method.
     */
    @Test
    public void testDuckTyping() {
        DuckTyping simple = new DuckTyping(queue);
        checkWriteRead(simple);
    }

    /**
     * Helper method to check the method writing and reading functionality.
     * @param simple An implementation of the Simple interface.
     */
    private void checkWriteRead(Simple simple) throws InvalidMarshallableException {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(32));
        Simple writer = wire.methodWriter(Simple.class);
        MethodReader reader = wire.methodReader(simple);
        final String superMario = "Mario";
        writer.hello(superMario);
        // writer =    "hello: Mario\n...\n"

        assertTrue(reader.readOne());
        assertEquals(1, queue.size());
        assertEquals(superMario, queue.poll());
    }

    /**
     * Simple interface with a single method.
     */
    interface Simple {
        void hello(String name);
    }

    /**
     * Another interface with the same method as Simple.
     */
    interface SimpleSameMethod {
        void hello(String name);
    }

    /**
     * Descendant interface that extends Simple.
     */
    interface SimpleDescendant extends Simple {
    }

    /**
     * Class implementing SimpleDescendant.
     */
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

    /**
     * Abstract class implementing SimpleDescendant.
     */
    private static abstract class SimpleAbstractDescendantClass implements SimpleDescendant {
    }

    /**
     * Concrete class extending SimpleAbstractDescendantClass.
     */
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

    /**
     * Class extending SimpleDescendantClass2 and implementing Simple.
     */
    private static class SimpleDescendantClass3 extends SimpleDescendantClass2 implements Simple {
        SimpleDescendantClass3(BlockingQueue<String> queue) {
            super(queue);
        }
    }

    /**
     * Class implementing multiple interfaces with the same method (duck typing).
     */
    private static class DuckTyping extends SimpleDescendantClass2 implements Simple, SimpleSameMethod {
        DuckTyping(BlockingQueue<String> queue) {
            super(queue);
        }
    }
}
