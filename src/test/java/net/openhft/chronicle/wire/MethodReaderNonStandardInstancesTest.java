/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.BooleanConsumer;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    @SuppressWarnings("deprecation")
    @Test
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
        assertFalse(reader instanceof VanillaMethodReader);

        assertTrue(reader.readOne());
        assertTrue(b.get());
    }

    /**
     * Test case to verify that a core class can be passed to MethodReader.
     * Similar to the above test but uses a BooleanConsumer core class.
     */
    @SuppressWarnings("deprecation")
    @Test
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
        assertFalse(reader instanceof VanillaMethodReader);

        assertTrue(reader.readOne());
        assertTrue(b.get());
    }

    /**
     * Test case to verify that a lambda expression can be passed to MethodReader.
     * It demonstrates how a lambda can be used to implement the reading functionality.
     */
    @SuppressWarnings("deprecation")
    @Test
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
        assertFalse(reader instanceof VanillaMethodReader);

        assertTrue(reader.readOne());
        assertTrue(b.get());
    }

    /**
     * Simple interface for testing.
     * It provides a single call method which the tests use for writing and reading messages.
     */
    interface MyInterface {
        void call();
    }
}
