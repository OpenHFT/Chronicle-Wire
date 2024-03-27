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
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

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
public class DefaultMethodHandlingTest extends WireTestCommon {

    /**
     * Executes tests for method writers and readers with the specified WireType.
     *
     * @param wireType The WireType to use for the test.
     */
    private static void doTest(WireType wireType) {
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
                "...\n", wire.toString());

        // Create a StringWriter to capture logging output.
        StringWriter sw = new StringWriter();

        // Create a method reader that logs to the StringWriter.
        MethodReader reader = wire.methodReader(Mocker.logging(WithDefault.class, "", sw));

        // Read the method calls and assert the expected outcomes.
        assertTrue(reader.readOne()); // Expecting true when reading method1.
        assertTrue(reader.readOne()); // Expecting true when reading method2.
        assertFalse(reader.readOne()); // Expecting false when no more methods to read.

        // Assert that the logged output matches the expected string.
        assertEquals("method1[one]\n" +
                "method2[two]\n", sw.toString().replace("\r", ""));
    }

    /**
     * Tests the method writers and readers using TEXT wire format.
     */
    @Test
    public void withDefault() {
        doTest(WireType.TEXT);
    }

    /**
     * Tests the method writers and readers using YAML_ONLY wire format.
     */
    @Test
    public void withDefaultYaml() {
        doTest(WireType.YAML_ONLY);
    }
}
