/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class WireInternalTest extends WireTestCommon {

    // Test the serialization and deserialization of a Throwable object using Wire's object method.
    @Test
    @DisplayName("Wire serialises Throwable using object method")
    public void testThrowableAsObject() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for throwable object test");

        // Create an elastic byte buffer for testing.
        final Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            // Initialize the BinaryWire for serialization/deserialization.
            final Wire wire = new BinaryWire(bytes);

            // Create a new exception for testing.
            final Exception exc = new Exception();

            // Serialize the exception using Wire.
            wire.write(() -> "exc").object(exc);

            // Deserialize the exception from Wire.
            final Throwable actual = (Throwable) wire.read("exc").object();

            // Compare the stack traces of the original and deserialized exceptions.
            StackTraceElement[] expectedST = exc.getStackTrace();
            StackTraceElement[] actualST = actual.getStackTrace();
            assertEquals(expectedST.length,
                    actualST.length,
                    "Stack trace length should match for throwable method");
        } finally {
            // Release the resources used by the byte buffer.
            bytes.releaseLast();
        }
    }

    // Test the serialization and deserialization of a Throwable using Wire's dedicated throwable method.
    @Test
    @DisplayName("Wire serialises Throwable using throwable method")
    public void testThrowable() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for throwable text test");

        // Similar setup to the previous test but uses TEXT wire type and the dedicated throwable methods.
        final Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            final Wire wire = WireType.TEXT.apply(bytes);

            final Exception exc = new Exception();

            wire.write(() -> "exc").throwable(exc);

            final Throwable actual = wire.read("exc").throwable(false);
            StackTraceElement[] expectedST = exc.getStackTrace();
            StackTraceElement[] actualST = actual.getStackTrace();
            assertEquals(expectedST.length,
                    actualST.length,
                    "Stack trace length should match after round trip");
        } finally {
            bytes.releaseLast();
        }
    }

    // Test the conversion of a size-prefixed binary message to text using Wire.
    @Test
    @DisplayName("Binary blobs convert to expected text output")
    public void testFromSizePrefixedBinaryToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        @NotNull Wire wire = new BinaryWire(bytes);
        wire.usePadding(true);

        // Create and write multiple documents using Wire.
        wire.writeDocument(true, w -> w
                .write(() -> "csp").text("csp://hello-world")
                .write(() -> "tid").int64(123456789));
        wire.writeDocument(false, w -> w.write(() -> "reply").marshallable(
                w2 -> w2.write(() -> "key").int16(1)
                        .write(() -> "value").text("Hello World")));
        wire.writeDocument(false, w -> w.write(() -> "reply").sequence(
                w2 -> {
                    w2.text("key");
                    w2.int16(2);
                    w2.text("value");
                    w2.text("Hello World2");
                }));
        wire.writeDocument(false, wireOut -> wireOut.writeEventName(() -> "userid").text("peter"));

        // Convert the size-prefixed binary blobs in the byte buffer to a text representation.
        String actual = Wires.fromSizePrefixedBlobs(bytes);

        // Validate the converted output against the expected string.
        assertEquals("--- !!meta-data #binary\n" +
                "csp: \"csp://hello-world\"\n" +
                "tid: 123456789\n" +
                "# position: 36, header: 0\n" +
                "--- !!data #binary\n" +
                "reply: {\n" +
                "  key: 1,\n" +
                "  value: Hello World\n" +
                "}\n" +
                "# position: 76, header: 1\n" +
                "--- !!data #binary\n" +
                "reply: [\n" +
                "  key,\n" +
                "  2,\n" +
                "  value,\n" +
                "  Hello World2\n" +
                "]\n" +
                "# position: 116, header: 2\n" +
                "--- !!data #binary\n" +
                "userid: peter\n",
                actual,
                "Binary blobs should convert to expected text output");

        // Release the resources used by the byte buffer.
        bytes.releaseLast();
    }
}
